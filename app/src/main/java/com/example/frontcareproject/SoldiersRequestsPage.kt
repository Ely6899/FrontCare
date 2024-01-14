package com.example.frontcareproject

import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SoldiersRequestsPage : AppCompatActivity() {

    //vars:
    private lateinit var donationsTable: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldiers_requests_page)

        // Find the TableLayout
        donationsTable = findViewById(R.id.donationsTable)

        // Make API GET request
        Thread {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/soldiersRequests")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverResponse = reader.readText()

                runOnUiThread {
                    handleSoldiersRequestsResponse(serverResponse)
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleSoldiersRequestsResponse(response: String) {
        // Parse the JSON response
        val jsonArray = JSONArray(response)

        // Iterate over JSON array and add rows to the table
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
