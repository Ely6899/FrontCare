// server/Server.kt

package server

import com.example.frontcareproject.R
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
import utils.GlobalVar
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import org.json.JSONArray
import org.json.JSONObject
// Entry point of the server application
val mysql_url = "jdbc:mysql://localhost:3306/frontcare"
val mysql_user = "root" //
val mysql_password = "root" // change to YOUR password
val userId = GlobalVar.userId

/*
TODO: RAZ - DOCUMENT ALL FUNCS
 */

fun main() {
    // Start an embedded Netty server on port 8080 and configure it with the defined module
    embeddedServer(Netty, port = 8080) {
        // Call the module function to configure the server application
        module()
    }.start(wait = true)
}

// Function to execute a SQL query and retrieve all users using plain JDBC
/*
TEMPLATE
 */
fun getAllUsers(): List<Map<String, Any>> {

    DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
        connection.prepareStatement("SELECT * FROM users where userid < 3").use { statement ->
            statement.executeQuery().use { resultSet ->
                val resultList = mutableListOf<Map<String, Any>>()
                while (resultSet.next()) {
                    val rowMap = mutableMapOf<String, Any>()
                    rowMap["userid"] = resultSet.getInt("userid")
                    rowMap["email"] = resultSet.getString("username")
                    rowMap["password"] = resultSet.getString("password")
                    resultList.add(rowMap)
                }
                return resultList
            }
        }
    }
}

fun getUserProfile(userId: String?): Map<String, Any> {
    DriverManager.getConnection(mysql_url, mysql_user, mysql_password).use { connection ->
        connection.prepareStatement("SELECT firstname,lastname,location,email_address,phone_number FROM users WHERE user_id = ?").use { statement ->
            // Set the value for the parameter in the prepared statement
            statement.setString(1, userId)

            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    val rowMap = mutableMapOf<String, Any>()
                    rowMap["firstname"] = resultSet.getString("firstname")
                    rowMap["lastname"] = resultSet.getString("lastname")
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

fun authenticateUser(username: String?, password: String?): Int? {

    var connection: Connection? = null
    var userId: Int? = null

    try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        val statement: PreparedStatement = connection.prepareStatement("SELECT user_id FROM users WHERE username = ? AND password = ?")
        /*
        TODO: RAZ - USE MD5 TO PASSWORD
         */
        statement.setString(1, username)
        statement.setString(2, password)
        val resultSet: ResultSet = statement.executeQuery()

        if (resultSet.next()) {
            userId = resultSet.getInt("user_id")
        }
    } finally {
        connection?.close()
    }

    return userId
}

/*
    TODO: ELY FIX YOUR FUNC YOU PUNK
 */
fun userRegistration(data: Map<String, String>): Int? {
    // Extract email and password from the received payload
    val userType = data["userType"]
    val firstName = data["firstName"]
    val lastName = data["lastName"]
    val email = data["email"]
    val password = data["password"]
    val username = data["userName"]
    val location = data["location"]
    // val phone = data["phone"]
    val phone="0501234567"

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
        statement.setString(5, password)
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

                val userId = authenticateUser(username, password)
/*
TODO : RAZ - ADD USERTYPE TO THE RETURN JSON
 */
                val responseMessage = if (userId != null) {
                    mapOf("message" to "Login successful", "userId" to userId)
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
/*
    TODO: ELY FIX YOUR FUNC YOU PUNK
 */
        post ("/api/register"){
            try {
                // Receive the JSON payload from the request and deserialize it to a Map<String, String>
                val request = call.receive<Map<String, String>>() // maybe change to Map<String, Any>
                println(request)

                val userId = userRegistration(request)

                val responseMessage = if (userId != null) {
                    mapOf("message" to "INSERT successfully", "userId" to userId)
                } else {
                    mapOf("message" to "Failed to INSERT")
                }

                // Respond with a message in JSON format
                call.respond(responseMessage)

            } catch (e: Exception) {
                // Handle exceptions related to request format and respond with BadRequest status
                call.respond(HttpStatusCode.BadRequest, "Invalid request format")
            }
        }

        get("/api/users") {
            try {
                // Fetch all users from the database
                val users = getAllUsers()

                // Respond with the list of users in JSON format
                call.respond(users)
            } catch (e: Exception) {
                // Handle exceptions related to database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching users from the database")
            }
        }

        get("/api/profile/{userId}") {
            try {
                // Retrieve the userId from the path parameters
                val userId = call.parameters["userId"]
                println(userId)
                // Fetch user profile data from the database based on userId
                val profileData = getUserProfile(userId)
                println(profileData)

                // Respond with the user profile data in JSON format
                call.respond(profileData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching user profile from the database")
            }
        }

    }
}
