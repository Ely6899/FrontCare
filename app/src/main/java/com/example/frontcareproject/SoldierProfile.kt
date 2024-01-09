package com.example.frontcareproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class SoldierProfile : AppCompatActivity() {
    private lateinit var firstNameData: TextView
    private lateinit var lastNameData: TextView
    private lateinit var emailData: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldier_profile)

        firstNameData = findViewById(R.id.ShowSoldierProfileFirstName)
        lastNameData = findViewById(R.id.ShowSoldierProfileLastName)
        emailData = findViewById(R.id.ShowSoldierProfileEmail)

        val data = intent.extras
        if (data != null){
            firstNameData.text = data.getString("First Name")
            lastNameData.text = data.getString("Last Name")
            emailData.text = data.getString("Email")
        }
    }
}