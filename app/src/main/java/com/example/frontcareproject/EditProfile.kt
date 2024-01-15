package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class EditProfile : AppCompatActivity() {

    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerLocation: Spinner
    private lateinit var btnEditProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etPhone = findViewById(R.id.etEditPhone)
        etEmail = findViewById(R.id.etEditEmail)
        etPassword = findViewById(R.id.etEditPassword)
        btnEditProfile = findViewById(R.id.btnEditProfile)

        //Enable location spinner
        spinnerLocation = findViewById(R.id.spinnerLocationEdit)
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            this,
            R.array.locations_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinnerLocation.adapter = adapter
        }

        //Get initial user data
        fetchProfileData()

        btnEditProfile.setOnClickListener{
            sendNewData()
        }
    }

    private fun sendNewData(){
        Thread  {
            try {
                //val userId = GlobalVar.userId // Replace with your logic to get the user ID
                val url = URL("http://${GlobalVar.serverIP}:8080/api/updateProfile")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                val jsonInputString = """
                            {"userId": "${GlobalVar.userId}", 
                            "phoneNumber": "${etPhone.text}",
                            "email_address": "${etEmail.text}",
                            "password": "${etPassword.text}",
                            "location": "${spinnerLocation.selectedItem}"}
                            """.trimIndent()

                // Send JSON as the request body
                val outputStream = connection.outputStream
                outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
                outputStream.close()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverAns = reader.readLine()

                runOnUiThread {
                    handleEditProfileRequest(serverAns)
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleEditProfileRequest(serverAns: String) {
        val jsonResponse = JSONObject(serverAns)
        if (jsonResponse.optString("message") == "Update successfully"){
            startActivity(Intent(this@EditProfile, Profile::class.java))
        }
        else
            Toast.makeText(this," Failed to update details", Toast.LENGTH_LONG).show()
    }

    private fun fetchProfileData() {
        Thread  {
            try {
                //val userId = GlobalVar.userId // Replace with your logic to get the user ID
                val url = URL("http://${GlobalVar.serverIP}:8080/api/profile/${GlobalVar.userId}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverAns = reader.readLine()

                runOnUiThread {
                    handleProfileResponse(serverAns)
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleProfileResponse(serverAns: String) {
        try {
            // Parse the JSON response
            val jsonResponse = JSONObject(serverAns)

            etPhone.setText(jsonResponse.optString("phone_number"))
            etEmail.setText(jsonResponse.optString("email_address"))

            val locationsArray = resources.getStringArray(R.array.locations_array)

            // on below line we are setting selection for our spinner to spinner position.
            spinnerLocation.setSelection(locationsArray.indexOf(jsonResponse.optString("location")))


        } catch (e: Exception) {
            // Handle the case where parsing the JSON fails
            // For example, show a Toast message or log an error
            // Toast.makeText(this, "Failed to parse server response", Toast.LENGTH_SHORT).show()
            println("Failed to parse server response. Error: ${e.message}")
        }
    }
}