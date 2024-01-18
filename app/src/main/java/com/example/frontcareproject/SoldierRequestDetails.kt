package com.example.frontcareproject

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import android.util.Log;
import org.json.JSONObject


class SoldierRequestDetails : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldier_request_details)

        // Retrieve JSON array from the intent
        val jsonStringList = intent.getStringArrayListExtra("jsonArray")
        val jsonArray = JSONArray()

        if (jsonStringList != null) {
            for (jsonString in jsonStringList) {
                jsonArray.put(JSONObject(jsonString))
            }
        }

        // Check if the array is not empty
        if (jsonArray.length() > 0) {
            // Get the first JSON object from the array
            val jsonObject = jsonArray.getJSONObject(0)

            // Retrieve data from the JSON object
            val requestDate = jsonObject.getString("request_date")
            val firstname = jsonObject.getString("firstname")
            val productName = jsonObject.getString("product_name")
            val quantity = jsonObject.getInt("quantity").toString()
            val pickupLocation = jsonObject.getString("pickup_location")

            Log.d("SoldierRequestDetails", "------------------------------- $requestDate -------------------")

            // Use the data to populate UI elements in SoldierRequestDetails activity
            val requestDateTextView: TextView = findViewById(R.id.requestDateTextView)
            val firstnameTextView: TextView = findViewById(R.id.firstnameTextView)
            val productNameTextView: TextView = findViewById(R.id.productNameTextView)
            val quantityTextView: TextView = findViewById(R.id.quantityTextView)
            val pickupLocationTextView: TextView = findViewById(R.id.pickupLocationTextView)

            requestDateTextView.text = "Request Date: $requestDate"
            firstnameTextView.text = "First Name: $firstname"
            productNameTextView.text = "Product Name: $productName"
            quantityTextView.text = "Quantity: $quantity"
            pickupLocationTextView.text = "Pickup Location: $pickupLocation"
        }
    }
}
