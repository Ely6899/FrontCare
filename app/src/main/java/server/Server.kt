// server/Server.kt

package server

import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

// Entry point of the server application
fun main() {
    // Start an embedded Netty server on port 8080 and configure it with the defined module
    embeddedServer(Netty, port = 8080) {
        // Call the module function to configure the server application
        module()
    }.start(wait = true)
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

                // Perform your authentication logic here (dummy logic for demonstration purposes)
                val responseMessage = if (email != null && password != null) {
                    // Example: Check if email and password are valid
                    "Login successful"
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
    }
}
