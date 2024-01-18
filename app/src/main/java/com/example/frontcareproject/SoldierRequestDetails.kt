package com.example.frontcareproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import org.json.JSONObject
import server.userId
import utils.GlobalVar
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


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

        // Get views
        val requestDateTextView: TextView = findViewById(R.id.requestDateTextView)
        val firstnameTextView: TextView = findViewById(R.id.firstnameTextView)
        val pickupLocationTextView: TextView = findViewById(R.id.pickupLocationTextView)
        val donateButtonView: Button = findViewById(R.id.donateButton)
        val productsTableView: TableLayout = findViewById(R.id.productsTable)

        // Add button listener
        donateButtonView.setOnClickListener(donate(requestId))

        // Display user data
        requestDateTextView.text = "Request Date: $requestDate"
        firstnameTextView.text = "First Name: $firstname"
        pickupLocationTextView.text = "Pickup Location: $pickupLocation"

        // Get products and put them in map
        var products = mutableMapOf<String, Int>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            products[obj.getString("product_name")] = obj.getInt("quantity")
        }

        // Display products in table
        displayProducts(productsTableView, products)
    }

    private fun donate(requestId: Int): View.OnClickListener {
        return View.OnClickListener {view ->
            //if you are using your phone instead of emulator you need to change the ip
            val url = URL("http://${GlobalVar.serverIP}:8080/api/donation")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Construct the JSON payload with email and password
            val jsonInputString = """{"userId": "$userId", "requestId": "$requestId"}"""

            // Send JSON as the request body
            val outputStream = connection.outputStream
            outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
            outputStream.close()

            // Get the response
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val serverAns = reader.readLine()

            // check for errors
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // Do something
            }

            reader.close()
            connection.disconnect()
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
