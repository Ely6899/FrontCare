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
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

// Entry point of the server application
val mysql_url = "jdbc:mysql://localhost:3306/frontcare_db" // Replace with your MySQL database URL
val mysql_user = "root" //
val mysql_password = "root" // change to YOUR password

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
                val request = call.receive<Map<String, String>>()

                // Extract email and password from the received payload
                val email = request["email"]
                val password = request["password"]

                val userId = authenticateUser(email, password)

                // Perform your authentication logic here (dummy logic for demonstration purposes)
                val responseMessage = if (userId != null) {
                    "Login successful. User ID: $userId"
                } else {
                    "Invalid email or password"
                }

                // Respond with a message in JSON format
                call.respond(mapOf("message" to responseMessage))
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

    }
}
