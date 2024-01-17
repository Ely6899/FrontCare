package com.example.frontcareproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

class DonorsEventsPage : AppCompatActivity() {

    //vars:
    private lateinit var eventsTable: TableLayout
    private lateinit var jsonArray: JSONArray  // Make the JSON array a class-level variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donors_events_page)

        // Find the TableLayout
        eventsTable = findViewById(R.id.eventsTable)

        // Make API GET request
        Thread {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/donorsEvents")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverResponse = reader.readText()

                // Parse the JSON response
                jsonArray = JSONArray(serverResponse)

                runOnUiThread {
                    handleDonorsEventsResponse()
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleDonorsEventsResponse() {
        // Iterate over JSON array and add rows to the table
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            addRowToTable(
                jsonObject.getString("event_id"),  // Use event_id for identification
                jsonObject.getString("event_date"),  // Add event_date to its own column
                jsonObject.getString("event_location"),
                jsonObject.getString("event_address"),
                jsonObject.getInt("remaining_spot").toString(),
                jsonObject.getString("product_name")
            )
        }
    }

    private fun addRowToTable(
        eventId: String,
        eventDate: String,
        eventLocation: String,
        eventAddress: String,
        remainingSpot: String,
        productName: String
    ) {
        val existingRow = eventsTable.findViewWithTag<TableRow>(eventId)

        if (existingRow != null) {
            // If row with the same event_id exists, append product info to the existing row
            appendProductInfoToRow(existingRow, productName)
        } else {
            // Create a new row
            val newRow = TableRow(this)
            newRow.tag = eventId // Set tag to event_id for identification

            // Set gray background for the TableRow
            newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

            // Add columns for each piece of information
            val columns = listOf(eventDate, eventLocation, eventAddress, remainingSpot, "$productName")
            for (columnData in columns) {
                val column = TextView(this)
                column.text = columnData
                column.gravity = android.view.Gravity.CENTER
                column.setPadding(8, 8, 8, 8)
                // Set black border for the TextView
                column.setBackgroundResource(R.drawable.tables_outline)
                newRow.addView(column)
            }

            // Add the button to the last column
            val detailsButton = Button(this)
            detailsButton.text = "Details"
            detailsButton.setOnClickListener {
                // Filter the JSON array based on event_id
                val filteredArray = (0 until jsonArray.length())
                    .map { jsonArray.getJSONObject(it) }
                    .filter { it.getString("event_id") == eventId }
                    .toTypedArray()

                // Handle button click, e.g., show details for the corresponding row
                // Start EventDetails activity and pass relevant information
                val intent = Intent(this, EventDetails::class.java).apply {
                    putExtra("jsonArray", filteredArray.toString())
                }
                startActivity(intent)
            }
            newRow.addView(detailsButton)

            eventsTable.addView(newRow)
        }
    }

    private fun appendProductInfoToRow(existingRow: TableRow, productName: String) {
        // Find the column for productName in the existing row
        val productInfoColumn = existingRow.getChildAt(4) as? TextView

        // Append new product info to the existing column
        productInfoColumn?.text = "${productInfoColumn?.text}, $productName"
    }
}
