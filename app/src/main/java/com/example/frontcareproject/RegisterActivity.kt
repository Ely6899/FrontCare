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

                profileIntent = Intent(this, Profile::class.java)

                dataBundle = Bundle()
                dataBundle.putString("Profile Type", selectedType.text.toString())
                dataBundle.putString("First Name", etFirstName.text.toString())
                dataBundle.putString("Last Name", etLastName.text.toString())
                dataBundle.putString("Email", etEmail.text.toString())
                if (selectType.checkedRadioButtonId == R.id.radioDonor)
                    dataBundle.putString("Location", etLocation.text.toString())


                profileIntent.putExtras(dataBundle)
                startActivity(profileIntent)
            }
        }


    }
}