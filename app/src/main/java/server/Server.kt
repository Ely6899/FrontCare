// server/Server.kt

package server

import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.math.BigInteger
import java.security.MessageDigest

// mysql server credentials
val mysql_url = "jdbc:mysql://uxd6gaqgeoenekcj:0CqlD3oWHl1SBg9lqWLJ@bm1cdufjqwhe4cgtldeh-mysql.services.clever-cloud.com:3306/bm1cdufjqwhe4cgtldeh"
val mysql_user = "uxd6gaqgeoenekcj"
val mysql_password = "0CqlD3oWHl1SBg9lqWLJ"


/*
TODO: RAZ - DOCUMENT ALL functions
 */

fun main() {
    // start an embedded Netty server on port 8080 and configure it with the defined module
    embeddedServer(Netty, port = 8080) {
        // call the module function to configure the server application
        module()
    }.start(wait = true)
}

//This function converts a string into a md5 hash
fun hashMD5(password: String): String {
    val md = MessageDigest.getInstance("MD5")
    val byteArray = md.digest(password.toByteArray())

    // Convert the byte array to a hexadecimal string
    val number = BigInteger(1, byteArray)
    var hashedPassword = number.toString(16)

    // Pad the hexadecimal string with leading zeros if needed
    while (hashedPassword.length < 32) {
        hashedPassword = "0$hashedPassword"
    }

    return hashedPassword
}

//This function gets userId and returns the user's profile data
fun getUserProfile(userId: String?): Map<String, Any> {
    DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
        connection.prepareStatement("SELECT is_soldier,firstname,lastname,username,location,email_address,phone_number FROM users WHERE user_id = ?").use { statement ->
            // Set the value for the parameter in the prepared statement
            statement.setString(1, userId)

            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    val rowMap = mutableMapOf<String, Any>()
                    rowMap["is_soldier"] = resultSet.getInt("is_soldier")
                    rowMap["firstname"] = resultSet.getString("firstname")
                    rowMap["lastname"] = resultSet.getString("lastname")
                    rowMap["userName"] = resultSet.getString("username")
                    rowMap["location"] = resultSet.getString("location")
                    rowMap["email_address"] = resultSet.getString("email_address")
                    rowMap["phone_number"] = resultSet.getString("phone_number")
                    rowMap
                } else {
                    // If no result is found, return an empty map or handle it as needed
                    emptyMap()
                }
            }
        }
    }
}

//This function gets username&password and check if the user is in the system, if so returns is ID and Type
fun authenticateUser(username: String?, password: String?): Pair<Int?, Int?>{

    var connection: Connection? = null
    var userId: Int? = null
    var userType: Int? = null

    try {

        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        val statement: PreparedStatement = connection.prepareStatement("SELECT user_id, is_soldier FROM users WHERE username = ? AND password = ?")

        val hashPassword = hashMD5(password.toString())
        statement.setString(1, username)
        statement.setString(2, hashPassword)
        val resultSet: ResultSet = statement.executeQuery()

        if (resultSet.next()) {
            userId = resultSet.getInt("user_id")
            userType = resultSet.getInt("is_soldier")
        }
    } finally {
        connection?.close()
    }

    return Pair(userId, userType)
}

//This function register a new user in the system
fun userRegistration(data: Map<String, String>): Int? {
    // Extract email and password from the received payload
    val userType = data["userType"]
    val firstName = data["firstName"]
    val lastName = data["lastName"]
    val email = data["email"]
    val password = data["password"]
    val username = data["userName"]
    var location = data["location"]
    val phone = data["phone"]

    val hashPassword = hashMD5(password.toString())

    //checks if usertype is soldier ,if true we dont fill location
    if(userType == "1")
    {
        location = "null"
    }
    var connection: Connection? = null
    var userId: Int? = null

    try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        val statement: PreparedStatement = connection.prepareStatement(
            "INSERT INTO users (is_soldier, firstName, lastName, username, password, location, email_address,phone_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS
        )

        statement.setInt(1, userType?.toInt() ?: 0)
        statement.setString(2, firstName)
        statement.setString(3, lastName)
        statement.setString(4, username)
        statement.setString(5, hashPassword)
        statement.setString(6, location)
        statement.setString(7, email)
        statement.setString(8, phone)

        // Execute the insert statement
        val affectedRows = statement.executeUpdate()

        if (affectedRows > 0) {
            // Retrieve the generated user ID
            val generatedKeys: ResultSet = statement.getGeneratedKeys()
            if (generatedKeys.next()) {
                userId = generatedKeys.getInt(1)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return userId

}

/*
TODO: REVIEW THIS FUNCTION WITH THE BOYS
 */
//This function returns all the soldiers requests ,for the soldier requests feed
fun getSoldiersRequests(): List<Map<String, Any>> {
    val resultList = mutableListOf<Map<String, Any>>()

    try {
        // Establish the database connection
        DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
            val sqlQuery = """
                SELECT
                    soldier_requests.request_id,
                    users.firstname,
                    products.product_name,
                    request_details.quantity,
                    soldier_requests.pickup_location,
                    soldier_requests.request_date,
                    users.email_address,
                    users.phone_number
                FROM
                    request_details
                JOIN
                    products ON products.product_id = request_details.product_id
                JOIN
                    soldier_requests ON soldier_requests.request_id = request_details.request_id
                JOIN
                    users ON soldier_requests.soldier_id = users.user_id
                WHERE soldier_requests.status = "open";
            """.trimIndent()

            // Create a prepared statement
            val statement: PreparedStatement = connection.prepareStatement(sqlQuery)

            // Execute the query
            val resultSet: ResultSet = statement.executeQuery()

            // Process the result set and populate the list of maps
            while (resultSet.next()) {
                val rowMap = mutableMapOf<String, Any>()
                rowMap["request_id"] = resultSet.getInt("request_id")
                rowMap["firstname"] = resultSet.getString("firstname")
                rowMap["product_name"] = resultSet.getString("product_name")
                rowMap["quantity"] = resultSet.getInt("quantity")
                rowMap["pickup_location"] = resultSet.getString("pickup_location")
                rowMap["request_date"] = resultSet.getString("request_date")
                rowMap["email_address"] = resultSet.getString("email_address")
                rowMap["phone_number"] = resultSet.getString("phone_number")
                resultList.add(rowMap)
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }

    return resultList
}

/*
 This function returns all the donors events for the donors events feed
 Unless,the donorId is different from 0 , if so it returns only the events this particular donor created , for the history events page
 */
fun getDonorsEvents(donorId: String?): List<Map<String, Any>> {
    val resultList = mutableListOf<Map<String, Any>>()
    var sqlFiller = "" // var that will be added to the sql query to determine if its for events history or donors events
    if(donorId != "0")
    {
        sqlFiller = "WHERE donation_events.donor_id = $donorId;"
    }

    try {
        // Establish the database connection
        DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
            val sqlQuery = """
                SELECT
                    donation_events.event_id,
                    donation_events.event_date,
                    donation_events.event_location,
                    donation_events.event_address,
                    donation_events.remaining_spot,
                    products.product_name,
                    users.firstname,
                    users.lastname,
                    users.email_address,
                    users.phone_number
                FROM
                    donation_events
                JOIN
                    event_details ON  donation_events.event_id = event_details.event_id
                JOIN
                    products ON products.product_id = event_details.product_id
                JOIN 
                    users ON users.user_id = donation_events.donor_id
                    $sqlFiller
            """.trimIndent()

            val statement: PreparedStatement = connection.prepareStatement(sqlQuery)
            val resultSet: ResultSet = statement.executeQuery()

            while (resultSet.next()) {
                val rowMap = mutableMapOf<String, Any>()
                rowMap["event_id"] = resultSet.getInt("event_id")
                rowMap["event_date"] = resultSet.getString("event_date")
                rowMap["event_location"] = resultSet.getString("event_location")
                rowMap["event_address"] = resultSet.getString("event_address")
                rowMap["remaining_spot"] = resultSet.getInt("remaining_spot")
                rowMap["product_name"] = resultSet.getString("product_name")
                rowMap["firstname"] = resultSet.getString("firstname")
                rowMap["lastname"] = resultSet.getString("lastname")
                rowMap["email_address"] = resultSet.getString("email_address")
                rowMap["phone_number"] = resultSet.getString("phone_number")
                resultList.add(rowMap)
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }

    return resultList
}

//This function returns the
fun getHistory(userId: String?,userType:String?): List<Map<String, Any>> {
    val resultList = mutableListOf<Map<String, Any>>()
    var sqlFiller = "" // var that will be added to the sql query to determine if its for events history or donors events

    when (userType) {
        "soldier" -> sqlFiller = "users ON soldier_requests.donor_id = users.user_id " +
                "WHERE soldier_requests.soldier_id = $userId;"
        "donor"-> sqlFiller = " users ON soldier_requests.soldier_id = users.user_id " +
                "WHERE soldier_requests.donor_id = $userId;"

        else -> {
            return resultList // if there is a problem return an empty result
        }
    }

    try {
        // Establish the database connection
        DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
            val sqlQuery = """
                SELECT
                    soldier_requests.request_id,
                    users.firstname,
                    users.lastname,
                    products.product_name,
                    request_details.quantity,
                    soldier_requests.request_date,
                    soldier_requests.close_date,
                    soldier_requests.status
                FROM
                    request_details
                JOIN
                    products ON products.product_id = request_details.product_id
                JOIN
                    soldier_requests ON soldier_requests.request_id = request_details.request_id
                LEFT JOIN
                $sqlFiller
            """.trimIndent()

            val statement: PreparedStatement = connection.prepareStatement(sqlQuery)
            val resultSet: ResultSet = statement.executeQuery()

            while (resultSet.next()) {
                val rowMap = mutableMapOf<String, Any>()
                rowMap["request_id"] = resultSet.getInt("request_id")

                // Check if firstname is null, and provide a default value
                val firstname: String? = resultSet.getString("firstname")
                rowMap["firstname"] = firstname ?: "Unknown"

                // Check if lastname is null, and provide a default value
                val lastname: String? = resultSet.getString("lastname")
                rowMap["lastname"] = lastname ?: "Unknown"

                rowMap["product_name"] = resultSet.getString("product_name")
                rowMap["quantity"] = resultSet.getInt("quantity")
                rowMap["request_date"] = resultSet.getString("request_date")
                val closeDate: String? = resultSet.getString("close_date")
                rowMap["close_date"] = closeDate ?: "null"
                rowMap["status"] = resultSet.getString("status")
                resultList.add(rowMap)
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }

    return resultList
}

fun getSoldierEventsHistory(userId: String?): List<Map<String, Any>> {
    val resultList = mutableListOf<Map<String, Any>>()

    try {
        // Establish the database connection
        DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
            val sqlQuery = """
                SELECT
                    donation_events.event_id,
                    users.firstname,
                    users.lastname,
                    donation_events.event_date,
                    donation_events.event_location,
                    donation_events.event_address
                FROM
                    donation_events
                JOIN
                    users ON  donation_events.donor_id = users.user_id
				JOIN 
					event_participants ON event_participants.event_id = donation_events.event_id
				WHERE event_participants.user_id = ?;
            """.trimIndent()

            // Create a prepared statement
            val statement: PreparedStatement = connection.prepareStatement(sqlQuery)
            statement.setInt(1, userId?.toInt() ?: 0)

            // Execute the query
            val resultSet: ResultSet = statement.executeQuery()

            // Process the result set and populate the list of maps
            while (resultSet.next()) {
                val rowMap = mutableMapOf<String, Any>()
                rowMap["event_id"] = resultSet.getInt("event_id")
                rowMap["firstname"] = resultSet.getString("firstname")
                rowMap["lastname"] = resultSet.getString("lastname")
                rowMap["event_date"] = resultSet.getString("event_date")
                rowMap["event_location"] = resultSet.getString("event_location")
                rowMap["event_address"] = resultSet.getString("event_address")
                resultList.add(rowMap)
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }

    return resultList
}
fun updateUserInformation(data: Map<String, String>): Boolean {
    val userId = data["userId"]
    val phoneNumber = data["phoneNumber"]
    val email = data["email_address"]
    val userName = data["userName"]
    val rawPassword = data["password"]
    val location = data["location"]
    var hashPassword = ""

    var sql = """
        UPDATE users
        SET phone_number = ?,
            email_address = ?,
            username = ?,
            location = ?
        WHERE user_id = ?;
    """.trimIndent()

    if(rawPassword != "")
    {
        sql = """
        UPDATE users
        SET phone_number = ?,
            email_address = ?,
            username = ?,
            location = ?,
            password = ?
        WHERE user_id = ?;
    """.trimIndent()

        // Hash the password using MD5
         hashPassword = hashMD5(rawPassword.toString())
    }

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)
        val statement = connection.prepareStatement(sql)

        statement.setString(1, phoneNumber)
        statement.setString(2, email)
        statement.setString(3, userName)
        statement.setString(4, location)
        if(rawPassword != "") {
            statement.setString(5, hashPassword)
            statement.setInt(6, userId?.toInt() ?: 0)
        }else
        {
            statement.setInt(5, userId?.toInt() ?: 0)
        }

        val rowsAffected = statement.executeUpdate()
        return rowsAffected > 0

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
}
fun donorDonation(data: Map<String, String>): Boolean {
    val userId = data["userId"]
    val requestId = data["requestId"]

    val sql = """
        UPDATE soldier_requests
        SET donor_id = ?,
            status = "pending"
        WHERE request_id = ?;
    """.trimIndent()

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, userId?.toInt() ?: 0)
        statement.setInt(2, requestId?.toInt() ?: 0)

        val rowsAffected = statement.executeUpdate()
        return rowsAffected > 0

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
}

/*
TODO: NEED TO CHECK IF THIS FUNC WORKS AFTER IDAN IS DONE
 */
fun eventRegistration(data: Map<String, String>): Boolean {
    val userId = data["userId"]
    val eventId = data["eventId"]

    val checkSql = "SELECT COUNT(*) FROM event_participants WHERE event_id = ? AND user_id = ?;"
    val insertSql = "INSERT INTO event_participants (event_id, user_id) VALUES (?, ?);"
    val updateSql = "UPDATE donation_events SET remaining_spot = (remaining_spot - 1) WHERE remaining_spot > 0 AND event_id = ?;"

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Check if the user is already registered for the event
        val checkStatement = connection.prepareStatement(checkSql)
        checkStatement.setInt(1, eventId?.toInt() ?: 0)
        checkStatement.setInt(2, userId?.toInt() ?: 0)
        val resultSet = checkStatement.executeQuery()

        /*
        resultSet.next(): checks if there is at least one row in the result set.
        resultSet.getInt(1) == 0: checks if the value in the first column of the current row is equal to 0.
         */
        if (resultSet.next() && resultSet.getInt(1) == 0) {
            // User is not registered for the event, proceed with insertion
            val insertStatement = connection.prepareStatement(insertSql)
            insertStatement.setInt(1, eventId?.toInt() ?: 0)
            insertStatement.setInt(2, userId?.toInt() ?: 0)
            val insertRowsAffected = insertStatement.executeUpdate()

            // Update donation_events
            val updateStatement = connection.prepareStatement(updateSql)
            updateStatement.setInt(1, eventId?.toInt() ?: 0)
            val updateRowsAffected = updateStatement.executeUpdate()

            // Commit the transaction if both statements were successful
            if (insertRowsAffected > 0 && updateRowsAffected > 0) {
                return true
            }
        } else {
            // User is already registered for the event, do not proceed
            println("User is already registered for the event.")
        }

    } catch (e: Exception) {
        e.printStackTrace()
        connection?.rollback() // Rollback the transaction in case of an exception
    } finally {
        connection?.close()
    }
    /*
       TODO: RAZ - CHECK THAT ROLLBACK CANCELS THE ACTION IN THE DB
     */
    connection?.rollback()
    return false
}

fun donationConfirmation(data: Map<String, String>): Map<String, Any> {
    val sqlResult = mutableMapOf<String, Any>()
    val userId = data["userId"]
    val requestId = data["requestId"]

    val updateSql = """
        UPDATE soldier_requests
        SET close_date = CURRENT_DATE(),
            status = 'closed'
        WHERE request_id = ? AND soldier_id= ?;
    """.trimIndent()

    val selectSql = """
        SELECT close_date, status
        FROM soldier_requests
        WHERE request_id = ? AND soldier_id = ?;
    """.trimIndent()

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Execute the update statement
        val updateStatement = connection.prepareStatement(updateSql)
        updateStatement.setInt(1, requestId?.toInt() ?: 0)
        updateStatement.setInt(2, userId?.toInt() ?: 0)
        updateStatement.executeUpdate()

        // Execute the select statement to retrieve the updated values
        val selectStatement = connection.prepareStatement(selectSql)
        selectStatement.setInt(1, requestId?.toInt() ?: 0)
        selectStatement.setInt(2, userId?.toInt() ?: 0)

        val resultSet = selectStatement.executeQuery()
        if (resultSet.next()) {
            sqlResult["close_date"] = resultSet.getString("close_date")
            sqlResult["status"] = resultSet.getString("status")
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return sqlResult
}

fun createSoldierRequest(data: Map<String, Any>): Boolean{

    val userId = data["userId"].toString()
    val location = data["location"].toString()
    val products = data["products"] as Map<Int, Int>

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Insert into soldier_requests table
        val insertSoldierRequestSQL = """
            INSERT INTO soldier_requests (soldier_id, pickup_location, request_date, status)
            VALUES (?, ?, CURRENT_DATE(), 'open');
        """.trimIndent()

        val insertSoldierRequestStatement = connection.prepareStatement(insertSoldierRequestSQL, 1)
        insertSoldierRequestStatement.setInt(1, userId.toInt())
        insertSoldierRequestStatement.setString(2, location)
        insertSoldierRequestStatement.executeUpdate()

        // Get the generated request_id
        val generatedKeys = insertSoldierRequestStatement.generatedKeys
        var requestId: Int? = null
        if (generatedKeys.next()) {
            requestId = generatedKeys.getInt(1)
        }

        if (requestId != null) {
            // Insert into request_details table for each product
            val insertRequestDetailsSQL = """
                INSERT INTO request_details (request_id, product_id, quantity)
                VALUES (?, ?, ?);
            """.trimIndent()

            val insertRequestDetailsStatement = connection.prepareStatement(insertRequestDetailsSQL)

            for ((productId, quantity) in products) {
                insertRequestDetailsStatement.setInt(1, requestId)
                insertRequestDetailsStatement.setInt(2, productId)
                insertRequestDetailsStatement.setInt(3, quantity)
                insertRequestDetailsStatement.executeUpdate()
            }

            return true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
}

fun getProducts(): MutableMap<String, String> {
    val productsMap = mutableMapOf<String, String>()
    try {
        // Establish the database connection
        DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
            val sqlQuery = "SELECT products.product_id,products.product_name FROM products;"
            val statement: PreparedStatement = connection.prepareStatement(sqlQuery)
            val resultSet: ResultSet = statement.executeQuery()

            // Process the result set and populate the list of maps
            while (resultSet.next()) {
                productsMap[resultSet.getInt("product_id").toString()] = resultSet.getString("product_name")
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }
    return productsMap
}


// Define the Ktor application module
fun Application.module() {
    // Install the ContentNegotiation feature with Jackson as the JSON serializer/deserializer
    install(ContentNegotiation) {
        jackson {
            // Jackson configuration if needed
        }
    }

    // Install the StatusPages feature to handle exceptions and return appropriate HTTP statuses
    install(StatusPages) {
        // Handle any Throwable (general exception) by responding with Internal Server Error status
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.localizedMessage)
        }
    }

    // Define the routing configuration for the application
    routing {
        // Define a route for handling POST requests to "/api/login"
        post("/api/login") {
            try {
                // Receive the JSON payload from the request and deserialize it to a Map<String, String>
                val request = call.receive<Map<String, String>>() // maybe change to Map<String, Any>

                // Extract email and password from the received payload
                val username = request["username"]
                val password = request["password"]

                // Returns a pair of userId and userType
                val userPair = authenticateUser(username, password)
                val userId = userPair.first
                val userType = userPair.second

                val responseMessage = if (userId != null && userType != null) {
                    mapOf("message" to "Login successful", "userId" to userId, "userType" to userType)
                } else {
                    mapOf("message" to "Invalid username or password")
                }

                // Respond with a message in JSON format
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }
        }

        post ("/api/register"){
            try {
                // Receive the JSON payload from the request and deserialize it to a Map<String, String>
                val request = call.receive<Map<String, String>>() // maybe change to Map<String, Any>


                val userId = userRegistration(request)

                val responseMessage = if (userId != null) {
                    mapOf("message" to "register successfully", "userId" to userId)
                } else {
                    mapOf("message" to "Failed to register")
                }

                // Respond with a message in JSON format
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }
        }

        post ("/api/updateProfile"){
            try {
                // Receive the JSON payload from the request and deserialize it to a Map<String, String>
                val request = call.receive<Map<String, String>>() // maybe change to Map<String, Any>
                val isUpdated = updateUserInformation(request) // True - Update successfully else False

                val responseMessage = if (isUpdated) {
                    mapOf("message" to "Update successfully")
                } else {
                    mapOf("message" to "Failed to Update")
                }

                // Respond with a message in JSON format
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }
        }

        post ("/api/donation"){
            try {
                val request = call.receive<Map<String, String>>() // maybe change to Map<String, Any>
                val isUpdated = donorDonation(request) // True - Update DB successfully else False

                val responseMessage = if (isUpdated) {
                    mapOf("message" to "Update successfully")
                } else {
                    mapOf("message" to "Failed to Update")
                }

                // Respond with a message in JSON format
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }
        }

        post ("/api/eventRegistration"){
            try {
                val request = call.receive<Map<String, String>>() // maybe change to Map<String, Any>
                val isRegistered = eventRegistration(request) // True - Update DB successfully else False

                val responseMessage = if (isRegistered) {
                    mapOf("message" to "Event registration successfully")
                } else {
                    mapOf("message" to "Failed to register")
                }

                // Respond with a message in JSON format
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }
        }

        post ("/api/donationConfirmation"){
            try {
                val request = call.receive<Map<String, String>>() // maybe change to Map<String, Any>
                val confirmationData = donationConfirmation(request) // True - Update DB successfully else False

                call.respond(confirmationData)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Confirmation failed")
            }
        }

        post ("/api/createSoldierRequest"){
            try {
                val request = call.receive<Map<String, Any>>() // maybe change to Map<String, Any>
                val confirmationData = createSoldierRequest(request) // True - Update DB successfully else False

                call.respond(confirmationData)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Confirmation failed")
            }
        }

        get("/api/profile/{userId}") {
            try {
                // Retrieve the userId from the path parameters
                val userId = call.parameters["userId"]
                val profileData = getUserProfile(userId)

                // Respond with the user profile data in JSON format
                call.respond(profileData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching user profile from the database")
            }
        }

        get("/api/soldiersRequests") {
            try {

                val requestsData = getSoldiersRequests()
                call.respond(requestsData)

            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching soldiers requests from the database")
            }
        }

        get("/api/donorsEvents") {
            try {
                val eventsData = getDonorsEvents("0")
                call.respond(eventsData)

            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching donors events from the database")
            }
        }

        get("/api/products") {
            try {
                val products = getProducts()
                call.respond(products)

            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching products from the database")
            }
        }

        get("/api/soldierRequestHistory/{userId}") {
            try {

                val userId = call.parameters["userId"]
                val historyData = getHistory(userId,"soldier")

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching history data from the database")
            }
        }

        get("/api/soldierEventsHistory/{userId}") {
            try {

                val userId = call.parameters["userId"]
                val historyData = getSoldierEventsHistory(userId)

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching history data from the database")
            }
        }

        get("/api/donorDonationHistory/{userId}") {
            try {

                val userId = call.parameters["userId"]
                val historyData = getHistory(userId,"donor")

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching history data from the database")
            }
        }

        get("/api/donorEventsHistory/{userId}") {
            try {

                val userId = call.parameters["userId"]
                val historyData = getDonorsEvents(userId)

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching history data from the database")
            }
        }

    }
}
