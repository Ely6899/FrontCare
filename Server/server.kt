import java.net.ServerSocket

fun main() {
    val serverSocket = ServerSocket(8080)
    println("Server started on port 8080")

    while (true) {
        val clientSocket = serverSocket.accept()
        Thread {
            handleClient(clientSocket)
        }.start()
    }
}

fun handleClient(clientSocket: java.net.Socket) {
    println("Client connected from: ${clientSocket.remoteSocketAddress}")

    val input = clientSocket.getInputStream().bufferedReader()
    val output = clientSocket.getOutputStream().bufferedWriter()

    while (true) {
        val request = input.readLine() ?: break
        when (request.trim().lowercase()) {
            "ping" -> {
                output.write("Pong\n")
                output.flush()
            }
            "quit" -> {
                output.write("Goodbye!\n")
                output.flush()
                break
            }
            else -> {
                output.write("Invalid command\n")
                output.flush()
            }
        }
    }

    clientSocket.close()
    println("Client disconnected")
}