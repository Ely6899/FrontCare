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
val mysql_url =
    "jdbc:mysql://uxd6gaqgeoenekcj:0CqlD3oWHl1SBg9lqWLJ@bm1cdufjqwhe4cgtldeh-mysql.services.clever-cloud.com:3306/bm1cdufjqwhe4cgtldeh"
val mysql_user = "uxd6gaqgeoenekcj"
val mysql_password = "0CqlD3oWHl1SBg9lqWLJ"


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
        connection.prepareStatement("SELECT is_soldier,firstname,lastname,username,location,email_address,phone_number FROM users WHERE user_id = ?")
            .use { statement ->
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

//This function gets username&password and checks if the user is in the system, if so returns their ID and Type
fun authenticateUser(username: String?, password: String?): Pair<Int?, Int?> {

    var connection: Connection? = null
    var userId: Int? = null
    var userType: Int? = null

    try {

        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        val statement: PreparedStatement =
            connection.prepareStatement("SELECT user_id, is_soldier FROM users WHERE username = ? AND password = ?")

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

//This function register a new user in the system and returns the userid
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
    if (userType == "1") {
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

//This function returns all the soldiers requests ,for the soldier requests page
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
 This function returns all the donors events for the donors events page
 Unless,the donorId is different from 0 , if so it returns only the events this particular donor created , for the history events page
 */
fun getDonorsEvents(donorId: String?): List<Map<String, Any>> {
    val resultList = mutableListOf<Map<String, Any>>()
    var sqlFiller = "WHERE donation_events.event_date >= CURRENT_DATE() AND remaining_spot > 0;" // var that will be added to the sql query to determine if its for events history or donors events

    //if donorid = 0 ,it means we want to get all the events for the events page , otherwise we only want to receive the events of a specific donor.
    if (donorId != "0") {
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

/*
 This function returns the the solider request history or donor donation history according to the input it receives
 if the usertype is soldier it returns solider request history, else donor donation history
 */
fun getHistory(userId: String?, userType: String?): List<Map<String, Any>> {
    val resultList = mutableListOf<Map<String, Any>>()

    val sqlFiller =
        when (userType) { // var that will be added to the sql query to determine if its for events history or donors events
            "soldier" -> "users ON soldier_requests.donor_id = users.user_id WHERE soldier_requests.soldier_id = ?"
            "donor" -> "users ON soldier_requests.soldier_id = users.user_id WHERE soldier_requests.donor_id = ?"
            else -> return resultList // if there is a problem return an empty result
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
                    soldier_requests.status,
                    soldier_requests.pickup_location,
                    users.email_address,
                    users.phone_number
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
            statement.setInt(1, userId?.toInt() ?: 0)

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
                rowMap["pickup_location"] = resultSet.getString("pickup_location")
                val emailAddress: String? = resultSet.getString("email_address")
                rowMap["email_address"] = emailAddress ?: "Unknown"
                val phoneNumber: String? = resultSet.getString("phone_number")
                rowMap["phone_number"] = phoneNumber ?: "Unknown"
                resultList.add(rowMap)
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }

    return resultList
}

//This function returns the history of the events a soldier registered to
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
                    users.email_address,
                    users.phone_number,
                    donation_events.event_date,
                    donation_events.event_location,
                    donation_events.event_address,
                    donation_events.remaining_spot,
                    products.product_name
                FROM
                    donation_events
                JOIN
                    users ON donation_events.donor_id = users.user_id
				JOIN 
					event_participants ON event_participants.event_id = donation_events.event_id
                JOIN
                    event_details ON  donation_events.event_id = event_details.event_id
                JOIN
                    products ON products.product_id = event_details.product_id
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
                rowMap["remaining_spot"] = resultSet.getInt("remaining_spot")
                rowMap["product_name"] = resultSet.getString("product_name")
                val emailAddress: String? = resultSet.getString("email_address")
                rowMap["email_address"] = emailAddress ?: "Unknown"
                val phoneNumber: String? = resultSet.getString("phone_number")
                rowMap["phone_number"] = phoneNumber ?: "Unknown"
                resultList.add(rowMap)
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }

    return resultList
}

//This function updates the user information on the DB
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

    //if the password value is not empty so we want to update the user's password
    if (rawPassword != "") {
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
        if (rawPassword != "") {
            statement.setString(5, hashPassword)
            statement.setInt(6, userId?.toInt() ?: 0)
        } else {
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

//This function updates on the DB which donor donated to a specific soldier
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

//This function register a soldier to an event
fun eventRegistration(data: Map<String, String>): Boolean {
    val userId = data["userId"]
    val eventId = data["eventId"]

    val checkSql = "SELECT COUNT(*) FROM event_participants WHERE event_id = ? AND user_id = ?;"
    val insertSql = "INSERT INTO event_participants (event_id, user_id) VALUES (?, ?);"
    val updateSql =
        "UPDATE donation_events SET remaining_spot = (remaining_spot - 1) WHERE remaining_spot > 0 AND event_id = ?;"

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
            //connection?.rollback() // Rollback the transaction in case of an exception
            return false
        }

    } catch (e: Exception) {
        e.printStackTrace()
        connection?.rollback() // Rollback the transaction in case of an exception
    } finally {
        connection?.close()
    }

    //connection?.rollback()
    return false
}

//This function updates on the DB a soldier's confirmation upon receiving a donation
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

//This function creates a new soldier request
fun createSoldierRequest(data: Map<String, Any>): Boolean {

    val userId = data["userId"].toString()
    val location = data["location"].toString()
    val products = data["products"] as Map<String, Int>

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
        var rowsAffected = insertSoldierRequestStatement.executeUpdate()

        if (rowsAffected <= 0) {
            return false
        }

        // Get the generated request_id
        val generatedKeys = insertSoldierRequestStatement.generatedKeys
        var requestId: Int? = null
        if (generatedKeys.next()) {
            requestId = generatedKeys.getInt(1)
        }


        if (requestId == null) {
            //connection?.rollback()
            return false
        }
        // Insert into request_details table for each product
        val insertRequestDetailsSQL = """
                INSERT INTO request_details (request_id, product_id, quantity)
                VALUES (?, ?, ?);
            """.trimIndent()

        val insertRequestDetailsStatement = connection.prepareStatement(insertRequestDetailsSQL)

        for ((productId, quantity) in products) {
            insertRequestDetailsStatement.setInt(1, requestId)
            insertRequestDetailsStatement.setInt(2, productId.toInt())
            insertRequestDetailsStatement.setInt(3, quantity)
            rowsAffected = insertRequestDetailsStatement.executeUpdate()
            if (rowsAffected <= 0) {
                //connection?.rollback()
                return false
            }
        }

        return true

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
}

//This function creates a new donor's event
fun createEvent(data: Map<String, Any>): Boolean {

    val userId = data["userId"].toString()
    val eventDate = data["eventDate"].toString()
    val eventLocation = data["eventLocation"].toString()
    val eventAddress = data["eventAddress"].toString()
    val eventSpots = data["eventSpots"].toString()
    val products = data["products"] as List<Int>

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Insert into donation_events table
        val insertDonationEventsSQL = """
            INSERT INTO donation_events (donor_id, event_date, event_location, event_address,remaining_spot)
            VALUES (?, ?, ?, ?, ? );
        """.trimIndent()

        val insertSoldierRequestStatement = connection.prepareStatement(insertDonationEventsSQL, 1)
        insertSoldierRequestStatement.setInt(1, userId.toInt())
        insertSoldierRequestStatement.setString(2, eventDate)
        insertSoldierRequestStatement.setString(3, eventLocation)
        insertSoldierRequestStatement.setString(4, eventAddress)
        insertSoldierRequestStatement.setInt(5, eventSpots.toInt())
        var rowsAffected = insertSoldierRequestStatement.executeUpdate()

        if (rowsAffected <= 0) {
            //connection?.rollback()
            return false
        }

        // Get the generated eventId
        val generatedKeys = insertSoldierRequestStatement.generatedKeys
        var eventId: Int? = null
        if (generatedKeys.next()) {
            eventId = generatedKeys.getInt(1)
        }

        if (eventId == null) {
            //connection?.rollback()
            return false
        }
        // Insert into event_details table
        val insertEventDetailsSQL = """
                INSERT INTO event_details (event_id, product_id)
                VALUES (?, ?);
            """.trimIndent()

        val insertRequestDetailsStatement = connection.prepareStatement(insertEventDetailsSQL)

        for (productId in products) {
            insertRequestDetailsStatement.setInt(1, eventId)
            insertRequestDetailsStatement.setInt(2, productId)
            rowsAffected = insertRequestDetailsStatement.executeUpdate()
            if (rowsAffected <= 0) {
                //connection?.rollback()
                return false
            }
        }

        return true

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
}

/*
This function deleted a soldier request from the DB
this can happen when a soldiers decided that they want to delete their request
*/
fun removeRequest(requestId: String?): Boolean {
    val deleteSoldierRequestSQL = "DELETE FROM soldier_requests WHERE request_id = ?"
    val deleteRequestDetailsSQL = "DELETE FROM request_details WHERE request_id = ?"

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Delete from request_details table
        val deleteRequestDetailsStatement = connection.prepareStatement(deleteRequestDetailsSQL)
        deleteRequestDetailsStatement.setInt(1, requestId?.toIntOrNull() ?: 0)
        val rowsAffectedRequestDetails = deleteRequestDetailsStatement.executeUpdate()

        // Delete from soldier_requests table
        val deleteSoldierRequestStatement = connection.prepareStatement(deleteSoldierRequestSQL)
        deleteSoldierRequestStatement.setInt(1, requestId?.toIntOrNull() ?: 0)
        val rowsAffectedSoldierRequests = deleteSoldierRequestStatement.executeUpdate()

        return if (rowsAffectedSoldierRequests > 0 && rowsAffectedRequestDetails > 0) {
            true
        } else {
            //connection?.rollback()
            false
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
}

//This function returns all the products ids and names from the DB
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
                productsMap[resultSet.getInt("product_id").toString()] =
                    resultSet.getString("product_name")
            }
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log or throw custom exception
        e.printStackTrace()
    }
    return productsMap
}

//This function updates a request details on the DB
fun updateRequest(data: Map<String, String>): Boolean {
    val requestId = data["request_id"].toString()
    val productsMap = data.filterKeys { it != "request_id" }
        .mapKeys { it.key.toIntOrNull() ?: 0 }
        .filter { it.key > 0 && it.value.toIntOrNull() != null }
        .mapValues { it.value.toInt() }

    if (productsMap.isEmpty()) {
        // If there are no valid product quantities, return false
        return false
    }
    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)
        connection?.autoCommit = false // Disable auto-commit to handle transactions

        for ((productId, quantity) in productsMap) {
            if (quantity == 0) {
                // If quantity is 0, delete the corresponding product from request_details
                val deleteProductSQL = """
                DELETE FROM request_details WHERE request_id = ? AND product_id = ?;
            """.trimIndent()
                val deleteStatement = connection.prepareStatement(deleteProductSQL)
                deleteStatement.setInt(1, requestId.toInt())
                deleteStatement.setInt(2, productId)
                val deleteRowsAffected = deleteStatement.executeUpdate()

                if (deleteRowsAffected <= 0) {
                    // If no rows were affected, rollback the transaction and return false
                    //connection.rollback()
                    return false
                }
            } else {
                // If quantity is greater than 0, update the quantity for the corresponding product
                val updateQuantitySQL = """
                UPDATE request_details SET quantity = ? WHERE request_id = ? AND product_id = ?;
            """.trimIndent()
                val updateStatement = connection.prepareStatement(updateQuantitySQL)
                updateStatement.setInt(1, quantity)
                updateStatement.setInt(2, requestId.toInt())
                updateStatement.setInt(3, productId)
                val updateRowsAffected = updateStatement.executeUpdate()

                if (updateRowsAffected <= 0) {
                    // If no rows were affected, insert the product into request_details
                    val insertProductSQL = """
                        INSERT INTO request_details (request_id, product_id, quantity) VALUES (?, ?, ?);
                    """.trimIndent()
                    val insertStatement = connection.prepareStatement(insertProductSQL)
                    insertStatement.setInt(1, requestId.toInt())
                    insertStatement.setInt(2, productId)
                    insertStatement.setInt(3, quantity)
                    val insertRowsAffected = insertStatement.executeUpdate()

                    if (insertRowsAffected <= 0) {
                        // If no rows were affected, rollback the transaction and return false
                        // connection.rollback()
                        return false
                    }
                }
            }
        }

        connection.commit() // Commit the transaction if all updates/deletes are successful
        return true

    } catch (e: Exception) {
        e.printStackTrace()
        connection?.rollback() // Rollback the transaction in case of any exception
    } finally {
        connection?.close()
    }

    return false
}

//This function cancel a registration of a soldier to an event
fun cancelEventRegistration(data: Map<String, String>): Boolean {
    val userId = data["userId"].toString()
    val eventId = data["eventId"].toString()

    val deleteEventParticipantSQL =
        "DELETE FROM event_participants WHERE event_id = ? AND user_id = ?;"
    val updateDonationEventsSQL =
        "UPDATE donation_events SET remaining_spot = (remaining_spot + 1) WHERE event_id = ?;"

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Delete from event_details table
        val deleteEventParticipantStatement = connection.prepareStatement(deleteEventParticipantSQL)
        deleteEventParticipantStatement.setInt(1, eventId.toInt())
        deleteEventParticipantStatement.setInt(2, userId.toInt())
        val rowsAffectedEventParticipant = deleteEventParticipantStatement.executeUpdate()

        // update remaining_spot on donation_events table
        val updateDonationEventsStatement = connection.prepareStatement(updateDonationEventsSQL)
        updateDonationEventsStatement.setInt(1, eventId.toInt())
        val rowsAffectedDonationEvents = updateDonationEventsStatement.executeUpdate()

        return if (rowsAffectedDonationEvents > 0 && rowsAffectedEventParticipant > 0) {
            true
        } else {
            //connection?.rollback() // Rollback the transaction in case of an exception
            false
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
}

fun soldierRequestReject(data: Map<String, String>): MutableMap<String, String> {
    val sqlResult = mutableMapOf<String, String>()
    val requestId = data["requestId"]

    val updateSql = """
        UPDATE soldier_requests
        SET donor_id = null,
            status = 'open'
        WHERE request_id = ? AND status = 'pending';
    """.trimIndent()

    val selectSql = """
        SELECT donor_id, status
        FROM soldier_requests
        WHERE request_id = ?;
    """.trimIndent()

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Execute the update statement
        val updateStatement = connection.prepareStatement(updateSql)
        updateStatement.setInt(1, requestId?.toInt() ?: 0)
        updateStatement.executeUpdate()

        // Execute the select statement to retrieve the updated values
        val selectStatement = connection.prepareStatement(selectSql)
        selectStatement.setInt(1, requestId?.toInt() ?: 0)

        val resultSet = selectStatement.executeQuery()
        if (resultSet.next()) {
            val donorId: String? = resultSet.getString("donor_id")
            sqlResult["firstname"] = donorId ?: "Unknown"
            sqlResult["status"] = resultSet.getString("status")
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return sqlResult
}

fun updateEvent(data: Map<String, String>): Boolean {
    val eventId = data["event_id"].toString()
    val eventLocation = data["event_location"]
    val eventAddress = data["event_address"]
    val productsMap = data.filterKeys { it != "event_id" && it != "event_location" && it != "event_address" }
        .mapKeys { it.key.toIntOrNull() ?: 0 }
        .filter { it.key > 0 && it.value.toIntOrNull() != null }
        .mapValues { it.value.toInt() }

    if (productsMap.isEmpty()) {
        // If there are no valid product quantities, return false
        return false
    }
    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)
        connection?.autoCommit = false // Disable auto-commit to handle transactions

        val updateQuantitySQL = """
        UPDATE donation_events
        SET event_location = ?,
            event_address = ?
        WHERE event_id = ?;
    """.trimIndent()
        val updateStatement = connection.prepareStatement(updateQuantitySQL)
        updateStatement.setString(1, eventLocation)
        updateStatement.setString(2, eventAddress)
        updateStatement.setInt(3, eventId.toInt())
        val updateRowsAffected = updateStatement.executeUpdate()
        var deleteRowsAffected = 0 //initialize var
        var insertRowsAffected = 0 //initialize var

        for ((productId, quantity) in productsMap) {
            if (quantity == 0) {
                // If quantity is 0, delete the corresponding product from request_details
                val deleteProductSQL = """
                DELETE FROM event_details WHERE event_id = ? AND product_id = ?;
            """.trimIndent()
                val deleteStatement = connection.prepareStatement(deleteProductSQL)
                deleteStatement.setInt(1, eventId.toInt())
                deleteStatement.setInt(2, productId)
                deleteRowsAffected = deleteStatement.executeUpdate()

            } else {

                // Check if the product and event combination already exists
                val checkExistenceSQL = """
                SELECT COUNT(*) FROM event_details
                WHERE event_id = ? AND product_id = ?;
            """.trimIndent()
                val checkExistenceStatement = connection.prepareStatement(checkExistenceSQL)
                checkExistenceStatement.setInt(1, eventId.toInt())
                checkExistenceStatement.setInt(2, productId)
                val resultSet = checkExistenceStatement.executeQuery()

                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    // The combination already exists, no need to insert
                    continue
                }

                // If no rows were affected, insert the product into request_details
                val insertProductSQL = """
                        INSERT INTO event_details (event_id, product_id) VALUES (?, ?);
                    """.trimIndent()
                val insertStatement = connection.prepareStatement(insertProductSQL)
                insertStatement.setInt(1, eventId.toInt())
                insertStatement.setInt(2, productId)
                insertRowsAffected = insertStatement.executeUpdate()

            }
        }

        if (deleteRowsAffected <= 0 && insertRowsAffected <= 0 && updateRowsAffected <=0) {
            // If no rows were affected, rollback the transaction and return false
            //connection.rollback()
            return false
        }

        connection.commit() // Commit the transaction if all updates/deletes are successful
        return true

    } catch (e: Exception) {
        e.printStackTrace()
        connection?.rollback() // Rollback the transaction in case of any exception
    } finally {
        connection?.close()
    }

    return false
}

fun removeEvent(eventId: String?): Boolean {
    val deleteDonorEventSQL = "DELETE FROM donation_events WHERE event_id = ?"
    val deleteEventDetailsSQL = "DELETE FROM event_details WHERE event_id = ?"
    val deleteEventParticipantsSQL = "DELETE FROM event_participants WHERE event_id = ?"

    var connection: Connection? = null

    try {
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        // Delete from event_details table
        val deleteEventDetailsStatement = connection.prepareStatement(deleteEventDetailsSQL)
        deleteEventDetailsStatement.setInt(1, eventId?.toIntOrNull() ?: 0)
        val rowsAffectedEventDetails = deleteEventDetailsStatement.executeUpdate()

        // Delete from donation_events table
        val deleteDonorEventStatement = connection.prepareStatement(deleteDonorEventSQL)
        deleteDonorEventStatement.setInt(1, eventId?.toIntOrNull() ?: 0)
        val rowsAffectedDonorEvent = deleteDonorEventStatement.executeUpdate()

        // Delete from event_participants table
        val deleteEventParticipantsStatement =
            connection.prepareStatement(deleteEventParticipantsSQL)
        deleteEventParticipantsStatement.setInt(1, eventId?.toIntOrNull() ?: 0)
        val rowsAffectedEventParticipants = deleteEventParticipantsStatement.executeUpdate()

        return if (rowsAffectedDonorEvent > 0 && rowsAffectedEventDetails > 0 && rowsAffectedEventParticipants >= 0) {
            true
        } else {
            //connection?.rollback()
            false
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.close()
    }

    return false
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
        //--------------------------------POST REQUESTS-----------------------------------------------------

        // Define a route for handling POST requests for user login
        post("/api/login") {
            try {
                // Receive the JSON payload from the request and deserialize it to a Map<String, String>
                val request = call.receive<Map<String, String>>()

                // Extract email and password from the received payload
                val username = request["username"]
                val password = request["password"]

                // Returns a pair of userId and userType
                val userPair = authenticateUser(username, password)
                val userId = userPair.first
                val userType = userPair.second

                val responseMessage = if (userId != null && userType != null) {
                    mapOf(
                        "message" to "Login successful",
                        "userId" to userId,
                        "userType" to userType
                    )
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

        // Define a route for handling POST requests for user registration
        post("/api/register") {
            try {
                // Receive the JSON payload from the request and deserialize it to a Map<String, String>
                val request =
                    call.receive<Map<String, String>>()

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

        // Define a route for handling POST requests for update user profile
        post("/api/updateProfile") {
            try {
                // Receive the JSON payload from the request and deserialize it to a Map<String, String>
                val request =
                    call.receive<Map<String, String>>()
                val isUpdated =
                    updateUserInformation(request) // True - Update successfully else False

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

        // Define a route for handling POST requests for a donor to donate
        post("/api/donation") {
            try {
                val request =
                    call.receive<Map<String, String>>()
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

        // Define a route for handling POST requests for a soldier to register to an event
        post("/api/eventRegistration") {
            try {
                val request =
                    call.receive<Map<String, String>>()
                val isRegistered =
                    eventRegistration(request) // True - Update DB successfully else False

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

        // Define a route for handling POST requests for a soldier to confirm a donation
        post("/api/donationConfirmation") {
            try {
                val request =
                    call.receive<Map<String, String>>()
                val confirmationData =
                    donationConfirmation(request) // True - Update DB successfully else False

                call.respond(confirmationData)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Confirmation failed")
            }
        }

        // Define a route for handling POST requests for a soldier to create a solider request
        post("/api/createSoldierRequest") {
            try {
                val request = call.receive<Map<String, Any>>()
                val respond =
                    createSoldierRequest(request) // True - Update DB successfully else False

                val responseMessage = if (respond) {
                    mapOf("message" to "Request created successfully")
                } else {
                    mapOf("message" to "Failed to create a Request")
                }

                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Request creation failed")
            }
        }

        // Define a route for handling POST requests for a donor to create a donation event
        post("/api/createEvent") {
            try {
                val request = call.receive<Map<String, Any>>()
                val respond = createEvent(request) // True - Update DB successfully else False

                val responseMessage = if (respond) {
                    mapOf("message" to "Event created successfully")
                } else {
                    mapOf("message" to "Failed to create an event")
                }

                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Event creation failed")
            }
        }

        // Define a route for handling POST requests for a soldier to remove a soldier request
        post("/api/removeRequest/{requestId}") {
            try {
                val requestId = call.parameters["requestId"]
                if (requestId == null) {
                    call.respond(mapOf("message" to "Invalid path parameter"))
                }
                val respond = removeRequest(requestId)

                val responseMessage = if (respond) {
                    mapOf("message" to "Removed successfully")
                } else {
                    mapOf("message" to "Failed to remove")
                }
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Request remove failed")
            }
        }

        // Define a route for handling POST requests for a soldier to update a soldier request
        post("/api/updateRequest") {
            try {
                val request =
                    call.receive<Map<String, String>>()
                val respond = updateRequest(request) // True - Update DB successfully else False

                val responseMessage = if (respond) {
                    mapOf("message" to "Update successfully")
                } else {
                    mapOf("message" to "Failed to update")
                }
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Update request failed")
            }
        }

        // Define a route for handling POST requests for a soldier to cancel is event registration
        post("/api/cancelEventRegistration") {
            try {
                val request = call.receive<Map<String, String>>()
                val respond =
                    cancelEventRegistration(request) // True - Update DB successfully else False

                val responseMessage = if (respond) {
                    mapOf("message" to "Canceled successfully")
                } else {
                    mapOf("message" to "Failed to cancel")
                }
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Cancel failed")
            }
        }

        // Define a route for handling POST requests for a soldier to reject donor's donation
        post("/api/soldierRequestReject") {
            try {
                val request = call.receive<Map<String, String>>()
                val respond =
                    soldierRequestReject(request) // True - Update DB successfully else False

                call.respond(respond)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Reject failed")
            }
        }

        // Define a route for handling POST requests for update an event details
        post("/api/updateEvent") {
            try {
                val request = call.receive<Map<String, String>>()
                val respond =
                    updateEvent(request) // True - Update DB successfully else False

                val responseMessage = if (respond) {
                    mapOf("message" to "Updated successfully")
                } else {
                    mapOf("message" to "Failed to update")
                }
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Update failed")
            }
        }

        // Define a route for handling POST requests to delete an event
        post("/api/removeEvent/{eventId}") {
            try {
                val eventId = call.parameters["eventId"]
                if (eventId == null) {
                    call.respond(mapOf("message" to "Invalid path parameter"))
                }
                val respond = removeEvent(eventId)

                val responseMessage = if (respond) {
                    mapOf("message" to "Removed successfully")
                } else {
                    mapOf("message" to "Failed to remove")
                }
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Event remove failed")
            }
        }

        //--------------------------------GET REQUESTS-----------------------------------------------------

        // Define a route for handling GET requests for getting user profile
        get("/api/profile/{userId}") {
            try {
                // Retrieve the userId from the path parameters
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(mapOf("message" to "Invalid path parameter"))
                }
                val profileData = getUserProfile(userId)

                // Respond with the user profile data in JSON format
                call.respond(profileData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching user profile from the database"
                )
            }
        }

        // Define a route for handling GET requests for getting all soldiers requests for soldier requests page
        get("/api/soldiersRequests") {
            try {
                val requestsData = getSoldiersRequests()
                call.respond(requestsData)

            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching soldiers requests from the database"
                )
            }
        }

        // Define a route for handling GET requests for getting all donors events for donors events page
        get("/api/donorsEvents") {
            try {
                val eventsData = getDonorsEvents("0")
                call.respond(eventsData)

            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching donors events from the database"
                )
            }
        }

        // Define a route for handling GET requests for getting all the products from DB
        get("/api/products") {
            try {
                val products = getProducts()
                call.respond(products)

            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching products from the database"
                )
            }
        }

        // Define a route for handling GET requests for getting a soldier requests history
        get("/api/soldierRequestHistory/{userId}") {
            try {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(mapOf("message" to "Invalid path parameter"))
                }

                val historyData = getHistory(userId, "soldier")

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching history data from the database"
                )
            }
        }

        // Define a route for handling GET requests for getting a soldier events participation history
        get("/api/soldierEventsHistory/{userId}") {
            try {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(mapOf("message" to "Invalid path parameter"))
                }

                val historyData = getSoldierEventsHistory(userId)

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching history data from the database"
                )
            }
        }

        // Define a route for handling GET requests for getting a donor donation history
        get("/api/donorDonationHistory/{userId}") {
            try {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(mapOf("message" to "Invalid path parameter"))
                }

                val historyData = getHistory(userId, "donor")

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching history data from the database"
                )
            }
        }

        // Define a route for handling GET requests for getting a donor's donation events history
        get("/api/donorEventsHistory/{userId}") {
            try {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(mapOf("message" to "Invalid path parameter"))
                }

                val historyData = getDonorsEvents(userId)

                call.respond(historyData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error fetching history data from the database"
                )
            }
        }

    }
}
