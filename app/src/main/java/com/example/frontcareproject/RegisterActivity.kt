package com.example.frontcareproject

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.frontcareproject.databinding.ActivityRegisterBinding


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding:ActivityRegisterBinding
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        val galleryUri = it
        try{
            binding.imgBtnPfp.setImageURI(galleryUri)
        }catch(e:Exception){
            e.printStackTrace()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        binding= ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imgBtnPfp.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }
}