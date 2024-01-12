package com.example.frontcareproject
import utils.GlobalVar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.content.Intent
import org.json.JSONObject
import android.widget.Toast
import androidx.core.content.ContextCompat



class LoginActivity : AppCompatActivity() {

    /*
    TODO: document code
     */
    private lateinit var typeText: TextView
    private lateinit var tverrormsg: TextView
    private lateinit var etEmail : EditText
    private lateinit var etPassword : EditText
    private lateinit var loginBtn: Button
    private lateinit var serverAns: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        typeText = findViewById(R.id.tvLoginType)
        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        loginBtn = findViewById(R.id.btnLogin)
        tverrormsg = findViewById(R.id.tvErrorMsg)

        serverAns = ""


        loginBtn.setOnClickListener {
            Thread {
                try {

                     //if you are using your phone instead of emulator you need to change the ip
                    val url = URL("http://10.0.2.2:8080/api/login")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    // Construct the JSON payload with email and password
                    val email = etEmail.text.toString()
                    val password = etPassword.text.toString()
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

    /*
    TODO: RAZ - NEED TO CHECK FOR INPUT validation
     */
    private fun handleServerResponse(response: String) {
        try {
            // Parse the JSON response
            val jsonResponse = JSONObject(response)

            // Check if the response indicates a successful login

            //optString is used ,This method returns an empty string if the key is not found.
            val message = jsonResponse.optString("message")
            val userId = jsonResponse.optString("userId")

            if (message == "Login successful" && userId.isNotEmpty()) {

                GlobalVar.userId = userId // set userid to global var

                // Navigate to the ProfileActivity
                val intent = Intent(this@LoginActivity, Profile::class.java)
                startActivity(intent)

                // Finish the LoginActivity to prevent going back on back press
                finish()

            } else {
                // Handle other cases or display an error message

                //Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                tverrormsg.text =  getString(R.string.login_failed_message)
                tverrormsg.setTextColor(ContextCompat.getColor(this, R.color.red))

            }
        } catch (e: Exception) {
            // Handle the case where parsing the JSON fails
            // For example, show a Toast message or log an error
            // Toast.makeText(this, "Failed to parse server response", Toast.LENGTH_SHORT).show()
            println("Failed to parse server response. Error: ${e.message}")
        }
    }



}
