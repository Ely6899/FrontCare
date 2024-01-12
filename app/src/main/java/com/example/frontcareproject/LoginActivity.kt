package com.example.frontcareproject

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {
    private lateinit var typeText: TextView
    private lateinit var tvemail: TextView
    private lateinit var tvpassword: TextView
    private lateinit var loginBtn: Button
    private lateinit var serverAns: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        typeText = findViewById(R.id.tvLoginType)
        tvemail = findViewById(R.id.etLoginEmail)
        tvpassword = findViewById(R.id.etLoginPassword)
        loginBtn = findViewById(R.id.btnLogin)

        serverAns = ""

        val extras = intent.extras
        if (extras != null) {
            val value: String? = extras.getString("token")

            //This looks scary cause android studio is afraid of hardcoded strings :\
            typeText.text = getString(R.string.log_in_welcome).format(value)
        }

        loginBtn.setOnClickListener {
            Thread {
                try {

                    val serverUrl = "http://10.0.2.2:8080/api/login"
                    val url = URL(serverUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    // Construct the JSON payload with email and password
                    val email = tvemail.text.toString()
                    val password = tvpassword.text.toString()
                    val jsonInputString = """{"email": "$email", "password": "$password"}"""

                    // Send JSON as the request body
                    val outputStream = connection.outputStream
                    outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
                    outputStream.close()

                    // Get the response
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    serverAns = reader.readLine()

                    runOnUiThread {
                        handleServerResponse(serverAns)
                    }

                    reader.close()
                    connection.disconnect()

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun handleServerResponse(response: String) {
        // Handle the received message from the server
        // For example, update UI elements with the received message
        println("Received message zibi bibi from server: $response")
    }
}
