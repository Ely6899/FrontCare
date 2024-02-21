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
import android.view.View
import android.widget.ImageView
import org.json.JSONObject
import android.widget.Toast
import androidx.core.content.ContextCompat



class LoginActivity : AppCompatActivity() {


    private lateinit var typeText: TextView
    private lateinit var tverrormsg: TextView
    private lateinit var etUserName : EditText
    private lateinit var etPassword : EditText
    private lateinit var loginBtn: Button
    //private lateinit var serverAns: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        typeText = findViewById(R.id.tvLoginType)
        etUserName = findViewById(R.id.etLoginUserName)
        etPassword = findViewById(R.id.etLoginPassword)
        loginBtn = findViewById(R.id.btnLogin)
        tverrormsg = findViewById(R.id.tvErrorMsg)

        loginBtn.setOnClickListener {
            Thread {
                try {

                     //if you are using your phone instead of emulator you need to change the ip
                    val url = URL("http://${GlobalVar.serverIP}:8080/api/login")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    // Construct the JSON payload with email and password
                    val username = etUserName.text.toString()
                    val password = etPassword.text.toString()
                    val jsonInputString = """{"username": "$username", "password": "$password"}"""

                    // Send JSON as the request body
                    val outputStream = connection.outputStream
                    outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
                    outputStream.close()

                    // Get the response
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val serverAns = reader.readLine()

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

        //making the actionBar functional:
        //making the back icon have a back functionality:
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val sideBarIcon = findViewById<ImageView>(R.id.sidebar_icon)
        sideBarIcon.visibility = View.INVISIBLE
        backIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, MainActivity::class.java))
        }
        // Set the callback
        GlobalVar.navigateCallback = { intent ->
            startActivity(intent)
            finish()
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
            val userType = jsonResponse.optInt("userType")

            if (message == "Login successful" && userId.isNotEmpty()) {

                /*
                TODO: RAZ - UPDATE USERTYPE GLOBAL VAR
                 */
                GlobalVar.userId = userId // set userid to global var
                GlobalVar.userType = userType

                // Navigate to the ProfileActivity
                val intent = Intent(this@LoginActivity, Profile::class.java)
                startActivity(intent)

                // Finish the LoginActivity to prevent going back on back press
                finish()

            } else {
                // Handle other cases or display an error message

                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
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
