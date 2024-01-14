package com.example.frontcareproject

import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class SoldiersRequestsPage : AppCompatActivity() {

    //vars:
    private lateinit var donationsTable: TableLayout

    // Sample JSON data
    private val jsonData = """
        [
            {
                "request_id": 1,
                "firstname": "JohnDoe",
                "product_name": "ProductA",
                "quantity": 3,
                "pickup_location": "LocationA",
                "request_date": "2024-01-15"
            },
            {
                "request_id": 1,
                "firstname": "JohnDoe",
                "product_name": "ProductB",
                "quantity": 2,
                "pickup_location": "LocationA",
                "request_date": "2024-01-15"
            },
            {
                "request_id": 2,
                "firstname": "JaneDoe",
                "product_name": "ProductC",
                "quantity": 1,
                "pickup_location": "LocationB",
                "request_date": "2024-01-16"
            }
            // ... more rows if there are additional requests with different products
        ]
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldiers_requests_page)

        // Find the TableLayout
        donationsTable = findViewById(R.id.donationsTable)

        // Parse JSON data
        val jsonArray = JSONArray(jsonData)

        // Example: Iterate over JSON array and add rows to the table
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            addRowToTable(
                jsonObject.getInt("request_id").toString(),
                jsonObject.getString("firstname"),
                jsonObject.getString("product_name"),
                jsonObject.getInt("quantity").toString(),
                jsonObject.getString("pickup_location"),
                jsonObject.getString("request_date")
            )
        }
    }

    private fun addRowToTable(
        requestId: String,
        firstname: String,
        productName: String,
        quantity: String,
        pickupLocation: String,
        requestDate: String
    ) {
        val existingRow = donationsTable.findViewWithTag<TableRow>(requestId)

        if (existingRow != null) {
            // If row with the same request_id exists, append product info to the existing row
            appendProductInfoToRow(existingRow, productName, quantity)
        } else {
            // Create a new row
            val newRow = TableRow(this)
            newRow.tag = requestId // Set tag to requestId for identification

            // Set gray background for the TableRow
            newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

            // Add columns for each piece of information
            val columns = listOf(requestId, firstname, "$productName - $quantity", pickupLocation, requestDate)
            for (columnData in columns) {
                val column = TextView(this)
                column.text = columnData
                column.gravity = android.view.Gravity.CENTER
                column.setPadding(8, 8, 8, 8)
                // Set black border for the TextView
                column.setBackgroundResource(R.drawable.tables_outline)
                newRow.addView(column)
            }

            donationsTable.addView(newRow)
        }
    }

    private fun appendProductInfoToRow(existingRow: TableRow, productName: String, quantity: String) {
        // Find the column for productName and quantity in the existing row
        val productInfoColumn = existingRow.getChildAt(2) as? TextView

        // Append new product info to the existing column
        productInfoColumn?.text = "${productInfoColumn?.text}, $productName - $quantity"
    }
}
