package com.example.frontcareproject

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import utils.GlobalVar

class EventDetails : AppCompatActivity() {
    private lateinit var eventDateTextView: TextView
    private lateinit var firstnameTextView: TextView
    private lateinit var eventAddressTextView: TextView
    private lateinit var remainingSpotsTextView: TextView
    private lateinit var contactTextView: TextView
    private lateinit var participateButton: Button
    private lateinit var productsListView: ListView
    private var eventId : Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        // Get views
        eventDateTextView = findViewById(R.id.eventDateTextView)
        firstnameTextView = findViewById(R.id.firstnameTextView)
        eventAddressTextView = findViewById(R.id.eventAddressTextView)
        contactTextView = findViewById(R.id.contactTextView)
        participateButton = findViewById(R.id.participateButton)
        productsListView = findViewById(R.id.productsListView)
        remainingSpotsTextView = findViewById(R.id.remainingSpotsTextView)

        // If we came from history page hide donate button
        val fromHistory : Boolean = intent.getBooleanExtra("fromHistory", false)
        if(fromHistory) {
            participateButton.visibility = View.INVISIBLE
        }

        // Extract event data from intent and display it
        displayEventData()

        // Add button listener
        participateButton.setOnClickListener{
            onParticipateButtonClick()
        }

        //making the actionBar functional:
        //making the back icon have a back functionality:
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            // If we came from history page we go back there
            val fromHistory : Boolean = intent.getBooleanExtra("fromHistory", false)
            if (fromHistory)
                GlobalVar.navigateToPage(Intent(this, UserEvents::class.java))
            else
            {
                GlobalVar.navigateToPage(Intent(this, DonorsEventsPage::class.java))
            }
        }
        //making the home icon to have a back to profile functionality:
        val homeIcon = findViewById<ImageView>(R.id.home_icon)
        homeIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, Profile::class.java))
        }
        // Set the callback
        GlobalVar.navigateCallback = { intent ->
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayEventData() {
        // Retrieve JSON array from the intent
        val jsonStringList = intent.getStringArrayListExtra("jsonArray")
        val jsonArray = JSONArray()

        // Cast back into JSONArray
        if (jsonStringList != null) {
            for (jsonString in jsonStringList) {
                jsonArray.put(JSONObject(jsonString))
            }
        }

        // Get the first JSON object from the array
        val jsonObject = jsonArray.getJSONObject(0)

        // Retrieve data from the JSON object
        eventId = jsonObject.getInt("event_id")
        val eventDate = jsonObject.getString("event_date")
        val firstname = jsonObject.getString("firstname")
        val eventAddress = jsonObject.getString("event_address")
        val emailAddress = jsonObject.getString("email_address")
        val phoneNumber = jsonObject.getString("phone_number")
        val remainingSpots = jsonObject.getString("remaining_spot")

        // Display user data
        eventDateTextView.text = "Event date: $eventDate"
        firstnameTextView.text = "Event organizer: $firstname"
        eventAddressTextView.text = "Event address: $eventAddress"
        remainingSpotsTextView.text = "Remaining spots: $remainingSpots"
        contactTextView.text = "Email: $emailAddress \nPhone number: $phoneNumber"

        // Get products and put them in map
        val products = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            products.add(obj.getString("product_name"))
        }

        // Initialize the ArrayAdapter with an empty list
        val adapter : ArrayAdapter<String> = ArrayAdapter(this@EventDetails, android.R.layout.simple_list_item_1)
        productsListView.adapter = adapter

        // Go over each item in products list and display them on screen
        for (product in products) {
            adapter.add(product)
            adapter.notifyDataSetChanged()
        }
    }

    private fun onParticipateButtonClick() {
        val url = "http://${GlobalVar.serverIP}:8080/api/eventRegistration"

        // Request in a new Coroutine that is destroyed after leaving this scope
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            // create request body
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = """{"userId": "${GlobalVar.userId}", "eventId": "$eventId"}"""
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
                // return to donor events page activity
                val intent = Intent(this@EventDetails, DonorsEventsPage::class.java)
                startActivity(intent)
            } else {
                println("Request failed with code: ${response.code}")
            }
        }
    }
}