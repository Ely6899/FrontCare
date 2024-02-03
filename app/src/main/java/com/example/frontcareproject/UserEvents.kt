package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.text.ParseException

class UserEvents : AppCompatActivity() {

    private lateinit var eventsTable: TableLayout
    private lateinit var createEventButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_events)

        eventsTable = findViewById(R.id.eventsTable)
        createEventButton = findViewById(R.id.createEventButton)

        // Go to create event page
        createEventButton.setOnClickListener{
            val intent = Intent(this, CreateEvent::class.java)
            startActivity(intent)
        }

        //Collapse products and remaining spots columns on initialization
        eventsTable.setColumnCollapsed(5, true)
        eventsTable.setColumnCollapsed(6, true)

        if(GlobalVar.userType == 1){fetchHistory("soldierEventsHistory")}
        else {fetchHistory("donorEventsHistory")}
    }

    private fun fetchHistory(apiRequest: String) {
        Thread  {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/$apiRequest/${GlobalVar.userId}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverAns = reader.readLine()

                runOnUiThread {
                    handleEventResponse(serverAns)
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleEventResponse(serverAns: String) {
        val jsonAnswer = JSONArray(serverAns)

        //Iterate through elements of the answer representing rows
        for (i in 0 until jsonAnswer.length()){
            val rowObject = jsonAnswer.getJSONObject(i)

            addRowToTable(rowObject)
        }
    }

    private fun addRowToTable(jsonObject: JSONObject) {
        val eventId = jsonObject.getString("event_id")
        val existingRow = eventsTable.findViewWithTag<TableRow>(eventId)

        if (existingRow != null && GlobalVar.userType == 0) {
            // If row with the same request_id exists, append product info to the existing row
            appendProductInfoToRow(existingRow, jsonObject.getString("product_name"))
        } else {
            // Create a new row
            val newRow = TableRow(this)
            newRow.tag = eventId // Set tag to request_id for identification

            // Set gray background for the TableRow
            newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

            val columns = listOf(
                "${jsonObject.getString("firstname")} ${jsonObject.getString("lastname")}" ,
                jsonObject.getString("event_date"),
                jsonObject.getString("event_location"),
                jsonObject.getString("event_address")
            ).toMutableList()

            if(GlobalVar.userType == 0){
                //Show the products and remaining spots columns if donor
                eventsTable.setColumnCollapsed(5, false)
                eventsTable.setColumnCollapsed(6, false)

                columns += listOf(
                    jsonObject.getString("product_name"),
                    jsonObject.getInt("remaining_spot").toString()
                )
            }

            val pattern = "yyyy-MM-dd" // Specify the pattern of the date string

            val formatter = SimpleDateFormat(pattern)
            var passedDate = true
            try {
                val parsedDate: Date = formatter.parse(jsonObject.getString("event_date"))!!
                passedDate = Date().after(parsedDate)
            } catch (e: ParseException) {
                println("Error parsing date: ${e.message}")
            }

            val editEventButton = Button(this)
            if(GlobalVar.userType == 1){
                if (passedDate)
                    editEventButton.isEnabled = false

                editEventButton.text = getString(R.string.leave_button)
                editEventButton.setOnClickListener {
                    handleEventRemoval(newRow)
                }
            }
            else{
                editEventButton.text = getString(R.string.edit_button_history_tables)
//                editEventButton.setOnClickListener {
//                    //handleEditEvent()
//                }
            }
            newRow.addView(editEventButton)

            // Add columns for each piece of information

            for (columnData in columns) {
                val column = TextView(this)
                column.text = columnData
                column.gravity = android.view.Gravity.CENTER
                column.setPadding(8, 8, 8, 8)
                // Set black border for the TextView
                column.setBackgroundResource(R.drawable.tables_outline)
                newRow.addView(column)
            }

            eventsTable.addView(newRow)
        }
    }

    private fun handleEventRemoval(rowToHandle: TableRow) {
        Thread  {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/cancelEventRegistration")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Construct the JSON payload with email and password
                val jsonInputString = """{"userId": "${GlobalVar.userId}", "eventId": "${rowToHandle.tag}"}"""

                // Send JSON as the request body
                val outputStream = connection.outputStream
                outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
                outputStream.close()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverAns = reader.readLine()

                runOnUiThread {
                    rowToHandle.visibility = View.GONE
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    /*
    * TODO(Implement editing of event for donor)
    * */
//    private fun handleEditEvent() {
//        startActivity(Intent(this@UserEvents, EditEvent::class.java))
//    }

    private fun appendProductInfoToRow(existingRow: TableRow, productName: String) {
        // Find the column for productName and quantity in the existing row
        val productInfoColumn = existingRow.getChildAt(5) as? TextView

        // Append new product info to the existing column
        "${productInfoColumn?.text}, $productName".also { productInfoColumn?.text = it }
    }
}


