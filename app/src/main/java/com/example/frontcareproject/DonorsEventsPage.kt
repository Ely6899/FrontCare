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
    private lateinit var eventsTable: TableLayout // the table from the xml file
    private lateinit var jsonArray: JSONArray  // the JSON that is being sent from the server

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donors_events_page)

        // Find the TableLayout from the xml:
        eventsTable = findViewById(R.id.eventsTable)

        // Make the API GET request from the server:
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

                // Process the response into a JSON array:
                jsonArray = JSONArray(serverResponse)

                runOnUiThread {
                    // fill up the table with the JSON array:
                    handleDonorsEventsResponse()
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle an exception in case one occurs:
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleDonorsEventsResponse() {
        // Iterate over the JSON array and add rows to the table
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            addRowToTable(
                // adding from each single JSON in the JSON array its values to a table row:
                jsonObject.getString("event_id"),
                jsonObject.getString("event_date"),
                jsonObject.getString("event_location"),
                jsonObject.getInt("remaining_spot").toString()
            )
        }
    }

    private fun addRowToTable(
        eventId: String, // Used for identification, so JSONs with the same event_id won't take more than one row
        eventDate: String,
        eventLocation: String,
        remainingSpot: String
    ) {
        // try to find a row with the same event_id, will be NULL if none is found:
        val existingRow = eventsTable.findViewWithTag<TableRow>(eventId)

        // If a row with the same event_id exists, then there is no need to make a new one
        // so only make a new row in case existingRow is null:
        if (existingRow == null) {

            // Create a new row:
            val newRow = TableRow(this)
            newRow.tag = eventId // Set tag to event_id for identification

            // Set background color for the TableRow from the defined table background color:
            newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

            // Add the details button to the first column for the current row:
            val detailsButton = Button(this)
            detailsButton.text = getString(R.string.details_buttons_text)

            // Define what happens on button click:
            detailsButton.setOnClickListener {
                // Filter the JSON array based on event_id, so only relevant JSONs will be sent to the details page,
                // then, reshape each JSON to a String. The filteredArray will now have an array of String-represented JSONs:
                val filteredArray = (0 until jsonArray.length())
                    .map { jsonArray.getJSONObject(it) }
                    .filter { it.getString("event_id") == eventId }
                    .map {
                        it.toString()
                    }

                // Start EventDetails activity and pass relevant information
                val intent = Intent(this, EventDetails::class.java).apply {
                    putStringArrayListExtra("jsonArray", ArrayList(filteredArray))
                }
                startActivity(intent)
            }

            // Add the button to the current new row:
            newRow.addView(detailsButton)

            // Add columns for each piece of information:
            val columns = listOf(eventDate, eventLocation, remainingSpot)
            for (columnData in columns) {
                val column = TextView(this)
                column.text = columnData
                column.gravity = android.view.Gravity.CENTER
                //column.setPadding(8, 8, 8, 8) TODO: maor - check if this line is needed or not
                // Set the defined border for the TextView:
                column.setBackgroundResource(R.drawable.tables_outline)
                newRow.addView(column)
            }

            eventsTable.addView(newRow)
        }
    }
}
