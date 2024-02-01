package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
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

class CreateEvent : AppCompatActivity() {
    private lateinit var productsListView : ListView
    private lateinit var radioGroup : RadioGroup
    private lateinit var createButton : Button
    private lateinit var maximumSpotsText : EditText
    private lateinit var eventDateText : EditText
    private lateinit var eventAddressText : EditText

    private var availableProducts : MutableMap<String, String> = mutableMapOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        productsListView = findViewById(R.id.productsListView)
        radioGroup = findViewById(R.id.radioGroup)
        createButton = findViewById(R.id.createButton)
        maximumSpotsText = findViewById(R.id.maximumSpotsText)
        eventDateText = findViewById(R.id.eventDateText)
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
                println("Request failed with code: ${response.code}")
            }
        }
    }

    private fun onCreateButtonClick() {
        // If none radio button is selected return
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) {
            return
        }

        val url = "http://${GlobalVar.serverIP}:8080/api/createEvent"

        // Find the radio button by ID
        val selectedRadioButton: RadioButton = findViewById(selectedRadioButtonId)

        // Get the texts of the inputs
        val location = selectedRadioButton.text.toString()
        val maximumSpots = maximumSpotsText.text.toString().toInt()
        val eventDate = eventDateText.text.toString()
        val eventAddress = eventAddressText.text.toString()

        // get checked items positions
        val checkedItemPositions = productsListView.checkedItemPositions
        val productsToSend = mutableListOf<Int>()

        // put their ids in a list
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
                println("Request failed with code: ${response.code}")
            }
        }
    }
}