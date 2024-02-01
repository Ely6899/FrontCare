package com.example.frontcareproject

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import utils.GlobalVar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class SoldierRequestDetails : AppCompatActivity() {
    private lateinit var requestDateTextView: TextView
    private lateinit var firstnameTextView: TextView
    private lateinit var pickupLocationTextView: TextView
    private lateinit var contactTextView: TextView
    private lateinit var donateButtonView: Button
    private lateinit var productsTableView: TableLayout
    private var requestId : Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldier_request_details)

        // Get views
        requestDateTextView = findViewById(R.id.requestDateTextView)
        firstnameTextView = findViewById(R.id.firstnameTextView)
        pickupLocationTextView = findViewById(R.id.pickupLocationTextView)
        contactTextView = findViewById(R.id.contactTextView)
        donateButtonView = findViewById(R.id.donateButton)
        productsTableView = findViewById(R.id.productsTable)

        // If we came from history page hide donate button
        val fromHistory : Boolean = intent.getBooleanExtra("fromHistory", false)
        if(fromHistory) {
            donateButtonView.visibility = View.INVISIBLE
        }

        // Extract request data from intent and display it
        displayRequestDetails()

        // Add button listener
        donateButtonView.setOnClickListener{
            onDonateButtonClick()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayRequestDetails() {
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
        requestId = jsonObject.getInt("request_id")
        val requestDate = jsonObject.getString("request_date")
        val firstname = jsonObject.getString("firstname")
        val pickupLocation = jsonObject.getString("pickup_location")
        val emailAddress = jsonObject.getString("email_address")
        val phoneNumber = jsonObject.getString("phone_number")

        // Display user data
        requestDateTextView.text = "Request Date: $requestDate"
        firstnameTextView.text = "Soldier Name: $firstname"
        pickupLocationTextView.text = "Pickup Location: $pickupLocation"
        contactTextView.text = "Email: $emailAddress \nPhone number: $phoneNumber"

        // Get products and put them in map
        val products = mutableMapOf<String, Int>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            products[obj.getString("product_name")] = obj.getInt("quantity")
        }

        // Go over each item in products map and display them
        for ((product, quantity) in products) {
            // create new row
            val newRow = TableRow(this)

            // Create text views
            val productView = createTextView(product)
            val quantityView = createTextView("$quantity")

            // Add text views to the row
            newRow.addView(productView)
            newRow.addView(quantityView)

            // Add the row to the table
            productsTableView.addView(newRow)
        }
    }

    private fun onDonateButtonClick() {
        val url = "http://${GlobalVar.serverIP}:8080/api/donation"

        // Request in a new Coroutine that is destroyed after leaving this scope
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            // create request body
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = """{"userId": "${GlobalVar.userId}", "requestId": "$requestId"}"""
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
                val intent = Intent(this@SoldierRequestDetails, SoldiersRequestsPage::class.java)
                startActivity(intent)
            } else {
                println("Request failed with code: ${response.code}")
            }
        }
    }

    private fun createTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setPadding(8, 8, 8, 8)

        // Make the text views take up the whole table
        val newLayoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f)
        newLayoutParams.gravity = Gravity.CENTER

        textView.layoutParams = newLayoutParams

        return textView
    }
}