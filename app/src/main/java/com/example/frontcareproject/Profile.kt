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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.drawable.Drawable
import android.widget.ImageView


class Profile : AppCompatActivity() {
    private lateinit var firstNameData: TextView
    private lateinit var lastNameData: TextView
    private lateinit var emailData: TextView
    private lateinit var locationRow: TableRow
    private lateinit var locationData: TextView
    private lateinit var phoneNumber: TextView
    private lateinit var profileImage: ImageView


    //Maor's addition to a redirect button:
    private lateinit var redirectBtnS: Button
    private lateinit var redirectBtnD: Button


    private lateinit var btnToEdit: Button
    private lateinit var btnToPostingsHistory: Button
    private lateinit var btnToEventsHistory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //making the actionBar functional:
        //making the back icon have a back functionality:
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        //making the home icon to have a back to profile functionality:
        val homeIcon = findViewById<ImageView>(R.id.home_icon)
        homeIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, Profile::class.java))
        }
        backIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, LoginActivity::class.java))
        }

        // Set the callback
        GlobalVar.navigateCallback = { intent ->
            startActivity(intent)
            finish()
        }


        firstNameData = findViewById(R.id.ShowProfileFirstName)
        lastNameData = findViewById(R.id.ShowProfileLastName)
        emailData = findViewById(R.id.ShowProfileEmail)
        locationRow = findViewById(R.id.LocationRow)
        locationData = findViewById(R.id.ShowProfileLocation)
        phoneNumber = findViewById(R.id.ShowPhoneNumber)
        profileImage = findViewById(R.id.imageView)

        fetchProfileData()

        // set profile image by usertype
        if (GlobalVar.userType == 1) {
            val defaultProfilePicture: Drawable? =
                ContextCompat.getDrawable(this, R.drawable.soldier_default_image_no_bg)
            profileImage.setImageDrawable(defaultProfilePicture)
        } else {
            val defaultProfilePicture: Drawable? =
                ContextCompat.getDrawable(this, R.drawable.donor_default_image_no_bg)
            profileImage.setImageDrawable(defaultProfilePicture)
        }


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
            // Navigate to the DonorsEventsPage
            val intent = Intent(this@Profile, DonorsEventsPage::class.java)
            startActivity(intent)
        }

        //disable buttons based on user's type: soldiers cant see SoldiersRequestsPage and donors cant see DonorsEventsPage
        if (GlobalVar.userType == 1) {
            redirectBtnS.visibility = View.INVISIBLE
        } else {
            redirectBtnD.visibility = View.INVISIBLE
        }

        btnToEdit = findViewById(R.id.btnGoToEdit)
        btnToEdit.setOnClickListener {
            startActivity(Intent(this@Profile, EditProfile::class.java))
        }

        btnToPostingsHistory = findViewById(R.id.btnPostingsHistory)
        btnToEventsHistory = findViewById(R.id.btnEventHistory)

        //Change button text according to user type connected
        if (GlobalVar.userType == 1) {
            btnToPostingsHistory.text = getString(R.string.requests_history_button)
        } else {
            btnToPostingsHistory.text = getString(R.string.donations_history_button)
        }

        btnToPostingsHistory.setOnClickListener {
            startActivity(Intent(this@Profile, UserPostings::class.java))
        }

        btnToEventsHistory.setOnClickListener {
            startActivity(Intent(this@Profile, UserEvents::class.java))
        }
    }



    private fun fetchProfileData() {
        Thread {
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


    private fun handleProfileResponse(response: String) {
        try {
            println("Server Response: $response")

            // Parse the JSON response
            val jsonResponse = JSONObject(response)

            // Check if the response indicates a successful login

            //optString is used ,This method returns an empty string if the key is not found.
            val isSoldier = jsonResponse.optString("is_soldier")
            val firstname = jsonResponse.optString("firstname")
            val lastname = jsonResponse.optString("lastname")
            val location = jsonResponse.optString("location")
            val emailAddress = jsonResponse.optString("email_address")
            val phone_number = jsonResponse.optString("phone_number")


            if (firstname.isNotEmpty() && lastname.isNotEmpty() && emailAddress.isNotEmpty() && phone_number.isNotEmpty()) {
                if (isSoldier == "0") {//donor
                    firstNameData.text = firstname
                    lastNameData.text = lastname
                    emailData.text = emailAddress
                    locationData.text = location
                    phoneNumber.text = phone_number
                } else {
                    //soldier
                    firstNameData.text = firstname
                    lastNameData.text = lastname
                    emailData.text = emailAddress
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
