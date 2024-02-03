package com.example.frontcareproject

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.util.isEmpty
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import utils.GlobalVar
import java.time.LocalDate

class CreateEvent : AppCompatActivity() {
    private lateinit var productsListView : ListView
    private lateinit var radioGroup : RadioGroup
    private lateinit var createButton : Button
    private lateinit var maximumSpotsText : EditText
    private lateinit var datePicker : DatePicker
    private lateinit var eventAddressText : EditText

    private var availableProducts : MutableMap<String, String> = mutableMapOf()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        productsListView = findViewById(R.id.productsListView)
        radioGroup = findViewById(R.id.radioGroup)
        createButton = findViewById(R.id.createButton)
        maximumSpotsText = findViewById(R.id.maximumSpotsText)
        datePicker = findViewById(R.id.datePicker)

        eventAddressText = findViewById(R.id.eventAddressText)

        // Get availableProducts from server
        getProducts()

        // Create button event listener
        createButton.setOnClickListener{
            onCreateButtonClick()
        }
    }
    private fun getProducts() {
        val url = "http://${GlobalVar.serverIP}:8080/api/products"

        val gson = Gson()
        val type = object : TypeToken<MutableMap<String, String>>() {}.type

        // Request in a new Coroutine that is destroyed after leaving this scope
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            // build the request
            val request = Request.Builder()
                .url(url)
                .build()

            // send request and put response in variable
            val response = client.newCall(request).execute()

            // check response code
            if (response.isSuccessful) {
                // need to run on new thread because we are changing the UI
                runOnUiThread {
                    // transform json back to map
                    availableProducts = gson.fromJson(response.body?.string(), type)

                    // use adapter to put the items in the list
                    val adapter = ArrayAdapter(
                        this@CreateEvent,
                        android.R.layout.simple_list_item_multiple_choice,
                        availableProducts.values.toList() // take only the names of the products.
                    )

                    // Apply the adapter to the list
                    productsListView.adapter = adapter
                }
            } else {
                Toast.makeText(this@CreateEvent, "Request failed with code: ${response.code}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onCreateButtonClick() {
        // Check if no location is picked.
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://${GlobalVar.serverIP}:8080/api/createEvent"

        // Find the radio button by ID
        val selectedRadioButton: RadioButton = findViewById(selectedRadioButtonId)

        // Get the texts of the inputs
        val location = selectedRadioButton.text.toString()
        val maximumSpotsString = maximumSpotsText.text.toString()
        val eventAddress = eventAddressText.text.toString()

        // Check text inputs
        if (maximumSpotsString.isEmpty() || eventAddress.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        // parse to Int
        val maximumSpots = maximumSpotsString.toInt()

        // Now check valid input
        if (maximumSpots < 0 || maximumSpots > 10000) {
            Toast.makeText(this, "Maximum spots should be between 1 and 10000", Toast.LENGTH_SHORT).show()
            return
        }

        // Get picked date
        val day = datePicker.dayOfMonth
        val month = datePicker.month + 1 // Month is zero-based
        val year = datePicker.year

        // Check date
        val currentDate = LocalDate.now()
        val pickedDate = LocalDate.of(year, month, day)
        if (pickedDate.isBefore(currentDate)) {
            Toast.makeText(this, "Please choose a future date", Toast.LENGTH_SHORT).show()
            return
        }

        // Format date
        val eventDate = "$year-$month-$day"

        // get checked items positions
        val checkedItemPositions = productsListView.checkedItemPositions

        // Check if no items are checked
        if (checkedItemPositions.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 item from the list", Toast.LENGTH_SHORT).show()
            return
        }

        // put their ids in a list (Position = ID)
        val productsToSend = mutableListOf<Int>()
        for (i in 0 until checkedItemPositions.size()) {
            val position = checkedItemPositions.keyAt(i)
            if (checkedItemPositions.valueAt(i)) {
                productsToSend.add(position)
            }
        }

        // convert Products map to jsonArray
        val gson = Gson()
        val jsonProducts = gson.toJson(productsToSend)

        // Request in a new Coroutine that is destroyed after leaving this scope
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            // create request body
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = """{"userId": ${GlobalVar.userId}, "eventDate": "$eventDate", "eventLocation": "$location", "eventAddress": "$eventAddress", "eventSpots": $maximumSpots, "products": $jsonProducts}""".trimMargin()
            val requestBody = json.toRequestBody(jsonMediaType)

            // build the request
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            // send request and put response in variable
            val response = client.newCall(request).execute()

            // check response code
            if (response.isSuccessful) {
                // return to requests page activity
                val intent = Intent(this@CreateEvent, UserEvents::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this@CreateEvent, "Request failed with code: ${response.code}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}