package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val registerButton = findViewById<Button>(R.id.btnRegisterDescription)
        val soldierLoginButton = findViewById<Button>(R.id.btnLogInSoldier)
        val donorLoginButton = findViewById<Button>(R.id.btnLogInDonor)

        val logInIntent = Intent(this, LoginActivity::class.java)
        val registerIntent = Intent(this, RegisterActivity::class.java)

        soldierLoginButton.setOnClickListener {

            //Used for passing data to the next activity
            logInIntent.putExtra("token", "soldier")
            startActivity(logInIntent)
        }

        donorLoginButton.setOnClickListener {
            logInIntent.putExtra("token", "donor")
            startActivity(logInIntent)
        }

        // Connects to register screen
        registerButton.setOnClickListener {
            startActivity(registerIntent)
        }
    }
}