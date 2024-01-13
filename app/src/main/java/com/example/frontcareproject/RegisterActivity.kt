package com.example.frontcareproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.frontcareproject.databinding.ActivityRegisterBinding
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class RegisterActivity : AppCompatActivity() {

    //Used for picture selection
    private lateinit var binding: ActivityRegisterBinding
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val galleryUri = it
        try {
            binding.imgBtnPfp.setImageURI(galleryUri)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    //RadioGroup of type we select on register
    private lateinit var selectType: RadioGroup

    //Form filling variables
    private lateinit var registerButton: Button
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etUserName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etLocation: EditText
    private lateinit var serverAns: String

    // Will carry filled data to next activity
    private lateinit var dataBundle: Bundle

    // Declare intent for entering profile
    private lateinit var profileIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //Picture selection
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imgBtnPfp.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        //Used for selecting profile picture

        //Used for extracting data of edit texts.
        registerButton = findViewById(R.id.btnRegister)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etUserName = findViewById(R.id.etUserName)
        etPassword = findViewById(R.id.etPassword)
        etLocation = findViewById(R.id.etDonationLocation)
        serverAns = ""

        selectType = findViewById(R.id.radioGrpSelectType)

        selectType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioSoldier) {
                etLocation.isEnabled = false
                etLocation.visibility = View.INVISIBLE
            }
            if (checkedId == R.id.radioDonor) {
                etLocation.isEnabled = true
                etLocation.visibility = View.VISIBLE
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

//                profileIntent = Intent(this, Profile::class.java)
//
//                dataBundle = Bundle()
//                dataBundle.putString("Profile Type", selectedType.text.toString())
//                dataBundle.putString("First Name", etFirstName.text.toString())
//                dataBundle.putString("Last Name", etLastName.text.toString())
//                dataBundle.putString("Email", etEmail.text.toString())
//                if (selectType.checkedRadioButtonId == R.id.radioDonor)
//                    dataBundle.putString("Location", etLocation.text.toString())
//
//
//                profileIntent.putExtras(dataBundle)
//                startActivity(profileIntent)

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
                        if (userType == "Donor") {
                            userType = "0"
                        } else {
                            userType = "1"
                        }
                        val firstName = etFirstName.text.toString()
                        val lastName = etLastName.text.toString()
                        val email = etEmail.text.toString()
                        val password = etPassword.text.toString()
                        val userName = etUserName.text.toString()
                        val location = etLocation.text.toString()
                        val jsonInputString = """
                            {"userType": "$userType", 
                            "firstName": "$firstName",
                            "lastName": "$lastName",
                            "email": "$email",
                            "password": "$password",
                            "userName": "$userName",
                            "location": "$location"}
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
    }

    private fun handleServerResponse(response: String) {
        try {
            // Parse the JSON response
            val jsonResponse = JSONObject(response)

            // Check if the response indicates a successful login

            //optString is used ,This method returns an empty string if the key is not found.
            val message = jsonResponse.optString("message")
            val userId = jsonResponse.optString("userId")

            if (message == "INSERT successfully" && userId.isNotEmpty()) {

                /*
                TODO: ELY - UPDATE USERTYPE GLOBAL VAR
                 */
                GlobalVar.userId = userId // set userid to global var

                // Navigate to the ProfileActivity
                val intent = Intent(this@RegisterActivity, Profile::class.java)
                startActivity(intent)

                // Finish the LoginActivity to prevent going back on back press
                finish()

            } else {
                // Handle other cases or display an error message
                //Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                /*
                TODO: ELY - CHECK IF ELSE HERE IS NEEDED
                 */


            }
        } catch (e: Exception) {
            // Handle the case where parsing the JSON fails
            // For example, show a Toast message or log an error
            // Toast.makeText(this, "Failed to parse server response", Toast.LENGTH_SHORT).show()
            println("Failed to parse server response. Error: ${e.message}")
        }
    }
}