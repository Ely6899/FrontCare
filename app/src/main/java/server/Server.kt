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
        connection.prepareStatement("SELECT userid, email FROM users WHERE userid = ?").use { statement ->
            // Set the value for the parameter in the prepared statement
            statement.setString(1, userId)

            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    val rowMap = mutableMapOf<String, Any>()
                    rowMap["userid"] = resultSet.getInt("userid")
                    rowMap["email"] = resultSet.getString("email")
                    rowMap
                } else {
                    // If no result is found, return an empty map or handle it as needed
                    emptyMap()
                }
            }
        }
    }
}



fun authenticateUser(email: String?, password: String?): Int? {

    var connection: Connection? = null
    var userId: Int? = null

    try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        val statement: PreparedStatement = connection.prepareStatement("SELECT userid FROM users WHERE email = ? AND password = ?")
        statement.setString(1, email)
        statement.setString(2, password)
        val resultSet: ResultSet = statement.executeQuery()

        if (resultSet.next()) {
            userId = resultSet.getInt("userid")
        }
    } finally {
        connection?.close()
    }

    return userId
}

/*
    TODO: ELY FIX YOUR FUNC YOU PUNK
 */
fun user_registration(userType: String?, firstName: String?, lastName: String?, email: String?, password: String?, userName: String?, location: String?): Int? {
    var connection: Connection? = null
    var userId: Int? = null

    try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        connection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password)

        val statement: PreparedStatement = connection.prepareStatement("INSERT INTO users_register_debug (userType, firstName, lastName, email, password, userName, location) VALUES (?, ?, ?, ?, ?, ?, ?)")
        statement.setString(1, userType)
        statement.setString(2, firstName)
        statement.setString(3, lastName)
        statement.setString(4, email)
        statement.setString(5, password)
        statement.setString(6, userName)
        statement.setString(7, location)

        // Use executeUpdate for INSERT operations
        val rowsAffected = statement.executeUpdate()

        // Check if any rows were affected
        if (rowsAffected > 0) {
            // Retrieve the generated keys if needed
            val generatedKeys = statement.generatedKeys
            if (generatedKeys.next()) {
                userId = generatedKeys.getInt(1)
            }
        }
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
                val email = request["email"]
                val password = request["password"]

                val userId = authenticateUser(email, password)

                val responseMessage = if (userId != null) {
                    mapOf("message" to "Login successful", "userId" to userId)
                } else {
                    mapOf("message" to "Invalid email or password")
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

                // Extract email and password from the received payload
                val userType = request["userType"]
                val firstName = request["firstName"]
                val lastName = request["lastName"]
                val email = request["email"]
                val password = request["password"]
                val userName = request["userName"]
                val location = request["location"]

                println(request)

                val userId = user_registration(userType, firstName,lastName,email,password,userName,location)

                val responseMessage = if (userId != null) {
                    mapOf("message" to "insert successfully", "userId" to userId)
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

                // Fetch user profile data from the database based on userId
                val profileData = getUserProfile(userId)

                // Respond with the user profile data in JSON format
                call.respond(profileData)
            } catch (e: Exception) {
                // Handle exceptions related to the database query and respond with InternalServerError status
                call.respond(HttpStatusCode.InternalServerError, "Error fetching user profile from the database")
            }
        }

    }
}
