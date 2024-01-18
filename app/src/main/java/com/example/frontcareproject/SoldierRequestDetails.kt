package com.example.frontcareproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import android.util.Log;
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TableLayout
import android.widget.TableRow
import org.json.JSONObject


class SoldierRequestDetails : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
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

        // Get the first JSON object from the array
        val jsonObject = jsonArray.getJSONObject(0)

        // Retrieve data from the JSON object
        val requestDate = jsonObject.getString("request_date")
        val firstname = jsonObject.getString("firstname")
        val pickupLocation = jsonObject.getString("pickup_location")

        // Get products and put them in map
        var products = mutableMapOf<String, Int>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            products[obj.getString("product_name")] = obj.getInt("quantity")
        }

        val productsTableView: TableLayout = findViewById(R.id.productsTable)

        for ((product, quantity) in products) {
            val newRow = TableRow(this)

            val productView = createTextView(product)
            val quantityView = createTextView("$quantity")

            newRow.addView(productView)
            newRow.addView(quantityView)

            productsTableView.addView(newRow)

        }

        // Use the data to populate UI elements in SoldierRequestDetails activity
        val requestDateTextView: TextView = findViewById(R.id.requestDateTextView)
        val firstnameTextView: TextView = findViewById(R.id.firstnameTextView)
        val pickupLocationTextView: TextView = findViewById(R.id.pickupLocationTextView)

        requestDateTextView.text = "Request Date: $requestDate"
        firstnameTextView.text = "First Name: $firstname"
        pickupLocationTextView.text = "Pickup Location: $pickupLocation"
    }

    private fun createTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setPadding(8, 8, 8, 8)

        val newLayoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f)
        newLayoutParams.gravity = Gravity.CENTER

        textView.layoutParams = newLayoutParams

        return textView
    }
}
