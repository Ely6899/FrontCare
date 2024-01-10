package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var registerButton: Button
    private lateinit var soldierLoginButton: Button
    private lateinit var donorLoginButton: Button

    //Declare different intents for each potential screen
    private lateinit var logInIntent: Intent
    private lateinit var registerIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerButton = findViewById(R.id.btnRegisterDescription)
        soldierLoginButton = findViewById(R.id.btnLogInSoldier)
        donorLoginButton = findViewById(R.id.btnLogInDonor)

        logInIntent = Intent(this, LoginActivity::class.java)

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
            registerIntent = Intent(this, RegisterActivity::class.java)
            startActivity(registerIntent)
        }
    }
}