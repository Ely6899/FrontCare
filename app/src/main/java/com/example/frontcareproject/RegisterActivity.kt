package com.example.frontcareproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.frontcareproject.databinding.ActivityRegisterBinding


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //Picture selection
        binding= ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imgBtnPfp.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        //Used for extracting data of edit texts.
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etUserName = findViewById<EditText>(R.id.etUserName)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etLocation = findViewById<EditText>(R.id.etDonationLocation)
        selectType = findViewById(R.id.radioGrpSelectType)

        registerButton.setOnClickListener{
            if (selectType.checkedRadioButtonId == -1){
                Toast.makeText(applicationContext,"Select type user for registration!", Toast.LENGTH_LONG).show()
            }else{
                val selectedType = findViewById<RadioButton>(selectType.checkedRadioButtonId)
                val typeString = selectedType.text.toString()

                val profileIntent = Intent(this, SoldierProfile::class.java)

                val dataBundle = Bundle()
                dataBundle.putString("First Name", etFirstName.text.toString())
                dataBundle.putString("Last Name", etLastName.text.toString())
                dataBundle.putString("Email", etEmail.text.toString())

                when (typeString){
                    "Donor" -> dataBundle.putString("Location", etLocation.text.toString())
                }

                profileIntent.putExtras(dataBundle)
                startActivity(profileIntent)

                TODO("Integration with DB")
            }

        }

    }
}