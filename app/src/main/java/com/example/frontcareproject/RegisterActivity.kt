package com.example.frontcareproject

import android.content.Intent
import android.os.Bundle
import android.view.View

import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class RegisterActivity : AppCompatActivity(){

    //RadioGroup of type we select on register
    private lateinit var selectType: RadioGroup

    //Form filling variables
    private lateinit var registerButton: Button
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etUserName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhone: EditText
    private lateinit var serverAns: String
    private lateinit var spinnerLocation: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //Used for selecting profile picture

        //Used for extracting data of edit texts.
        registerButton = findViewById(R.id.btnRegister)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etUserName = findViewById(R.id.etUserName)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        selectType = findViewById(R.id.radioGrpSelectType)

        //Enable location spinner
        spinnerLocation = findViewById(R.id.spinnerLocationSelect)
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

        //Hide location spinner upon creating page
        spinnerLocation.visibility = View.INVISIBLE

        //Enable or disable spinner based on user type selection
        selectType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioSoldier) {
                spinnerLocation.isEnabled = false
                spinnerLocation.visibility = View.INVISIBLE
            }
            if (checkedId == R.id.radioDonor) {
                spinnerLocation.isEnabled = true
                spinnerLocation.visibility = View.VISIBLE
            }
        }

        registerButton.setOnClickListener {
            if (selectType.checkedRadioButtonId == -1) {
                Toast.makeText(
                    this, "Select user type for registration!",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                val selectedType = findViewById<RadioButton>(selectType.checkedRadioButtonId)

                Thread {
                    try {

                        val serverUrl = "http://${GlobalVar.serverIP}:8080/api/register"
                        val url = URL(serverUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.doOutput = true

                        // Construct the JSON payload with register credentials
                        var userType = selectedType.text.toString()
                        userType = if (userType == "Donor") {
                            "0"
                        } else {
                            "1"
                        }
                        val firstName = etFirstName.text.toString()
                        val lastName = etLastName.text.toString()
                        val email = etEmail.text.toString()
                        val password = etPassword.text.toString()
                        val userName = etUserName.text.toString()
                        val location = spinnerLocation.selectedItem.toString()
                        val phone = etPhone.text.toString()
                        val jsonInputString = """
                            {"userType": "$userType", 
                            "firstName": "$firstName",
                            "lastName": "$lastName",
                            "email": "$email",
                            "password": "$password",
                            "userName": "$userName",
                            "location": "$location",
                            "phone": "$phone"}
                            """.trimIndent()

                        // Send JSON as the request body
                        val outputStream = connection.outputStream
                        outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
                        outputStream.close()

                        // Get the response
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        serverAns = reader.readLine()

                        runOnUiThread {
                            handleServerResponse(serverAns, userType)
                        }

                        reader.close()
                        connection.disconnect()

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }

        //making the actionBar functional:
        //making the back icon have a back functionality:
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val homeIcon = findViewById<ImageView>(R.id.home_icon)
        homeIcon.visibility = View.INVISIBLE
        backIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, MainActivity::class.java))
        }
        // Set the callback
        GlobalVar.navigateCallback = { intent ->
            startActivity(intent)
            finish()
        }
    }

    private fun handleServerResponse(response: String, userType: String) {
        try {
            // Parse the JSON response
            val jsonResponse = JSONObject(response)

            // Check if the response indicates a successful login

            //optString is used ,This method returns an empty string if the key is not found.
            val message = jsonResponse.optString("message")
            val userId = jsonResponse.optString("userId")

            if (message == "register successfully" && userId.isNotEmpty()) {
                GlobalVar.userId = userId // set userid to global var
                GlobalVar.userType = userType.toInt() // set user type to global var

                // Navigate to the ProfileActivity
                val intent = Intent(this@RegisterActivity, Profile::class.java)
                startActivity(intent)

                // Finish the RegisterActivity to prevent going back on back press
                finish()

            } else {
                // Handle other cases or display an error message
                Toast.makeText(this, "Register failed", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            // Handle the case where parsing the JSON fails
            // For example, show a Toast message or log an error
            // Toast.makeText(this, "Failed to parse server response", Toast.LENGTH_SHORT).show()
            println("Failed to parse server response. Error: ${e.message}")
        }
    }
}