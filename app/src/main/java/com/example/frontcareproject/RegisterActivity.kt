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
import com.example.frontcareproject.databinding.ActivityRegisterBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class RegisterActivity : AppCompatActivity() {

    //Used for picture selection
    private lateinit var binding:ActivityRegisterBinding
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        val galleryUri = it
        try{
            binding.imgBtnPfp.setImageURI(galleryUri)
        }catch(e:Exception){
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
        binding= ActivityRegisterBinding.inflate(layoutInflater)
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
            if (checkedId == R.id.radioSoldier){
                etLocation.isEnabled = false
                etLocation.visibility = View.INVISIBLE
            }
            if(checkedId == R.id.radioDonor){
                etLocation.isEnabled = true
                etLocation.visibility = View.VISIBLE
            }
        }

        registerButton.setOnClickListener{
            if (selectType.checkedRadioButtonId == -1){
                Toast.makeText(this,"Select user type for registration!",
                    Toast.LENGTH_SHORT)
                    .show()
            }else{
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
                        val serverUrl = "http://10.0.2.2:8080/api/register"
                        val url = URL(serverUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.doOutput = true

                        // Construct the JSON payload with register credentials
                        val userType = selectedType.text.toString()
                        val firstName = etFirstName.text.toString()
                        val lastName = etLastName.text.toString()
                        val email = etEmail.text.toString()
                        val password = etPassword.text.toString()
                        val userName = etLastName.text.toString()
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
        // Handle the received message from the server
        // For example, update UI elements with the received message
        println("Received message from server: $response")

        if (response == "Done") {
            Toast.makeText(this, "Registration complete", Toast.LENGTH_SHORT).show()
            //startActivity(Intent(this, Profile::class.java))
        }
        else{
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
        }
        //println("Received message from server: $response")
    }
}