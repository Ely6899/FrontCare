package com.example.frontcareproject

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import android.view.Gravity
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldier_request_details)

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
        val requestId = jsonObject.getInt("request_id")
        val requestDate = jsonObject.getString("request_date")
        val firstname = jsonObject.getString("firstname")
        val pickupLocation = jsonObject.getString("pickup_location")
        val emailAddress = jsonObject.getString("email_address")
        val phoneNumber = jsonObject.getString("phone_number")

        // Get views
        val requestDateTextView: TextView = findViewById(R.id.requestDateTextView)
        val firstnameTextView: TextView = findViewById(R.id.firstnameTextView)
        val pickupLocationTextView: TextView = findViewById(R.id.pickupLocationTextView)
        val contactTextView: TextView = findViewById(R.id.contactTextView)
        val donateButtonView: Button = findViewById(R.id.donateButton)
        val productsTableView: TableLayout = findViewById(R.id.productsTable)

        // Display user data
        requestDateTextView.text = "Request Date: $requestDate"
        firstnameTextView.text = "Soldier Name: $firstname"
        pickupLocationTextView.text = "Pickup Location: $pickupLocation"
        contactTextView.text = "Email: $emailAddress \nPhone number: $phoneNumber"

        // Get products and put them in map
        var products = mutableMapOf<String, Int>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            products[obj.getString("product_name")] = obj.getInt("quantity")
        }

        // Display products in table
        displayProducts(productsTableView, products)

        // Add button listener
        donateButtonView.setOnClickListener{
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
    }

    private fun displayProducts(productsTableView: TableLayout, products: Map<String, Int>) {
        // Go over each item in products map
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