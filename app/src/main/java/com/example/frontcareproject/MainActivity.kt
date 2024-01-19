package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var registerButton: Button
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerButton = findViewById(R.id.btnRegisterDescription)
        loginButton = findViewById(R.id.btnLogIn)

        //Connects to login screen
        loginButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }

        // Connects to register screen
        registerButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
        }
    }
}