package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import utils.GlobalVar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class Profile : AppCompatActivity() {
    private lateinit var firstNameData: TextView
    private lateinit var lastNameData: TextView
    private lateinit var emailData: TextView
    private lateinit var locationRow: TableRow
    private lateinit var locationData: TextView
    private lateinit var phoneNumber: TextView

    //Maor's addition to a redirect button:
    private lateinit var redirectBtnS: Button
    private lateinit var redirectBtnD: Button

    private lateinit var btnToEdit: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firstNameData = findViewById(R.id.ShowProfileFirstName)
        lastNameData = findViewById(R.id.ShowProfileLastName)
        emailData = findViewById(R.id.ShowProfileEmail)
        locationRow = findViewById(R.id.LocationRow)
        locationData = findViewById(R.id.ShowProfileLocation)
        phoneNumber = findViewById(R.id.ShowPhoneNumber)

//        val data = intent.extras
//        if (data != null){
//            firstNameData.text = data.getString("First Name")
//            lastNameData.text = data.getString("Last Name")
//            emailData.text = data.getString("Email")
//
//            if (data.getString("Profile Type") == "Donor")
//                locationData.text = data.getString("Location")
//            else
//                locationRow.visibility = View.GONE
//        }

        fetchProfileData()

        //Maor's addition to custom redirecting buttons:
        redirectBtnS = findViewById(R.id.btnSoldiersRequests)
        redirectBtnS.setOnClickListener()
        {
            // Navigate to the SoldiersRequestsPage
            val intent = Intent(this@Profile, SoldiersRequestsPage::class.java)
            startActivity(intent)
        }
        redirectBtnD = findViewById(R.id.btnDonorsList)
        redirectBtnD.setOnClickListener()
        {
            // Navigate to the SoldiersRequestsPage
            val intent = Intent(this@Profile, DonorsEventsPage::class.java)
            startActivity(intent)
        }

        btnToEdit = findViewById(R.id.btnGoToEdit)
        btnToEdit.setOnClickListener{
            startActivity(Intent(this@Profile, EditProfile::class.java))
        }

    }

    private fun fetchProfileData() {
        Thread  {
            try {
                val userId = GlobalVar.userId // Replace with your logic to get the user ID
                val url = URL("http://${GlobalVar.serverIP}:8080/api/profile/$userId")
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


    /*
    TODO: RAZ - NEED TO UPDATE THE FIELDS ACCORDING TO THE DB
     */
    private fun handleProfileResponse(response: String) {
        try {
            println("Server Response: $response")

            // Parse the JSON response
            val jsonResponse = JSONObject(response)

            // Check if the response indicates a successful login

            //optString is used ,This method returns an empty string if the key is not found.
            val is_soldier = jsonResponse.optString("is_soldier")
            val firstname = jsonResponse.optString("firstname")
            val lastname = jsonResponse.optString("lastname")
            val location = jsonResponse.optString("location")
            val email_address = jsonResponse.optString("email_address")
            val phone_number = jsonResponse.optString("phone_number")


            if (firstname.isNotEmpty() && lastname.isNotEmpty() && email_address.isNotEmpty() && phone_number.isNotEmpty()) {
                if(is_soldier == "0")
                {//donor
                    firstNameData.text = firstname
                    lastNameData.text = lastname
                    emailData.text = email_address
                    locationData.text = location
                    phoneNumber.text = phone_number
                }
                else{
                    //soldier
                    firstNameData.text = firstname
                    lastNameData.text = lastname
                    emailData.text = email_address
                    phoneNumber.text = phone_number
                    val profileLocationTextView = findViewById<TextView>(R.id.ProfileLocation)
                    profileLocationTextView.visibility = View.GONE // hide location field

                }

            } else {
                // Handle other cases or display an error message
                //Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Handle the case where parsing the JSON fails
            // For example, show a Toast message or log an error
            // Toast.makeText(this, "Failed to parse server response", Toast.LENGTH_SHORT).show()
            println("Failed to parse server response. Error: ${e.message}")
        }
    }
}
