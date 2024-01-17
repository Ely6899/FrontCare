package com.example.frontcareproject

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SoldierRequestDetails : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldier_request_details)

        // Retrieve data from the intent
        val requestDate = intent.getStringExtra("requestDate")
        val firstname = intent.getStringExtra("firstname")
        val productName = intent.getStringExtra("productName")
        val quantity = intent.getStringExtra("quantity")
        val pickupLocation = intent.getStringExtra("pickupLocation")

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
