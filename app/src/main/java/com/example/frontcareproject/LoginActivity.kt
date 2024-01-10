package com.example.frontcareproject

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var typeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        typeText = findViewById(R.id.tvLoginType)

        val extras = intent.extras
        if (extras != null) {
            val value : String? = extras.getString("token")

            //This looks scary cause android studio is afraid of hardcoded strings :\
            typeText.text = getString(R.string.log_in_welcome).format(value)
        }
    }
}