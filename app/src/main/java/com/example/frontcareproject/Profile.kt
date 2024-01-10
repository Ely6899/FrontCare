package com.example.frontcareproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TableRow
import android.widget.TextView

class Profile : AppCompatActivity() {
    private lateinit var firstNameData: TextView
    private lateinit var lastNameData: TextView
    private lateinit var emailData: TextView
    private lateinit var locationRow: TableRow
    private lateinit var locationData: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firstNameData = findViewById(R.id.ShowProfileFirstName)
        lastNameData = findViewById(R.id.ShowProfileLastName)
        emailData = findViewById(R.id.ShowProfileEmail)
        locationRow = findViewById(R.id.LocationRow)
        locationData = findViewById(R.id.ShowProfileLocation)


        val data = intent.extras
        if (data != null){
            firstNameData.text = data.getString("First Name")
            lastNameData.text = data.getString("Last Name")
            emailData.text = data.getString("Email")

            if (data.getString("Profile Type") == "Donor")
                locationData.text = data.getString("Location")
            else
                locationRow.visibility = View.GONE
        }
    }
}