package com.example.frontcareproject

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
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

    private lateinit var userEvents: JSONArray

    private lateinit var optionsArray: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_events)

        eventsTable = findViewById(R.id.eventsTable)
        createEventButton = findViewById(R.id.createEventButton)
        optionsArray = arrayOf("Options", "Details", "Edit", "Leave")

        if(GlobalVar.userType == 1){ //Soldier
            createEventButton.visibility = View.GONE
        }

        // Go to create event page
        createEventButton.setOnClickListener{
            val intent = Intent(this, CreateEvent::class.java)
            startActivity(intent)
        }

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
                userEvents = JSONArray(serverAns)

                runOnUiThread {
                    //Iterate through elements of the answer representing rows
                    for (i in 0 until userEvents.length()){
                        val jsonObject = userEvents.getJSONObject(i)
                        addRowToTable(jsonObject)
                    }
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    @SuppressLint("SimpleDateFormat", "ResourceAsColor")
    private fun addRowToTable(jsonObject: JSONObject) {
        val eventId = jsonObject.getString("event_id")
        val existingRow = eventsTable.findViewWithTag<TableRow>(eventId)

        if (existingRow == null) { //Create a new row
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

            //Filter the array for requests which match the requestId of the row.
            val filteredArray = (0 until userEvents.length())
                .map { userEvents.getJSONObject(it) }
                .filter { it.getString("event_id") == newRow.tag.toString() }
                .map {
                    it.toString()
                }

            val pattern = "yyyy-MM-dd" // Specify the pattern of the date string
            val formatter = SimpleDateFormat(pattern)
            var passedDate = true
            try {
                //Check if the event is outdated. Used for preventing from leaving outdated events for soldiers.
                val parsedDate: Date = formatter.parse(jsonObject.getString("event_date"))!!
                passedDate = Date().after(parsedDate)
            } catch (e: ParseException) {
                println("Error parsing date: ${e.message}")
            }

            val optionsSpinner = Spinner(this)
            if(GlobalVar.userType == 1) { //Soldier events
                val soldierOptions:Array<String> = if(passedDate) {
                    arrayOf(optionsArray[0], optionsArray[1])
                } else{ //With leave option if date is not expired
                    arrayOf(optionsArray[0], optionsArray[1], optionsArray[3])
                }

                optionsSpinner.adapter = ArrayAdapter(
                    this,
                    R.layout.spinner_item,
                    R.id.text_view, soldierOptions)
            }
            else{ //Donor
                val donorOptions:Array<String> = if(passedDate) {
                    arrayOf(optionsArray[0], optionsArray[1])
                } else{ //With edit option if date is not expired
                    arrayOf(optionsArray[0], optionsArray[1], optionsArray[2])
                }

                optionsSpinner.adapter = ArrayAdapter(
                    this,
                    R.layout.spinner_item,
                    R.id.text_view, donorOptions)
            }

            optionsSpinner.setBackgroundResource(R.drawable.tables_outline)
            optionsSpinner.setBackgroundColor(R.color.spinnerColor)

            optionsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (parent!!.getItemAtPosition(position).toString()) {
                        "Details" -> { //Details
                            val detailsIntent = Intent(this@UserEvents, EventDetails::class.java)
                            detailsIntent.putStringArrayListExtra("jsonArray", ArrayList(filteredArray))
                            detailsIntent.putExtra("fromHistory", true)
                            startActivity(detailsIntent)
                        }

                        "Edit" -> { //Edit
                            val editIntent = Intent(this@UserEvents, EditEvent::class.java)
                            editIntent.putExtra("event_id", newRow.tag.toString())
                            editIntent.putStringArrayListExtra("jsonArray", ArrayList(filteredArray))
                            startActivity(editIntent)
                        }

                        "Leave" -> {
                            handleEventRemoval(newRow)
                        }
                    }
                }
            }

            newRow.addView(optionsSpinner)

            // Add columns for each piece of information
            for (columnData in columns) {
                val column = TextView(this)
                column.text = columnData
                column.gravity = android.view.Gravity.CENTER
                column.setPadding(6, 6, 6, 6)
                column.setTextColor(Color.BLACK)
                // Set black border for the TextView
                column.setBackgroundResource(R.drawable.tables_outline)
                // Put column data to the row.
                newRow.addView(column)
            }
            //Add the entire event row to the table.
            eventsTable.addView(newRow)
        }
    }

    //Handles soldier leaving a specific event.
    private fun handleEventRemoval(rowToHandle: TableRow) {
        Thread  {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/cancelEventRegistration")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

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
                    //Remove the row after server successfully confirms removal from the DB
                    if(JSONObject(serverAns).optString("message") == "Canceled successfully")
                        eventsTable.removeView(rowToHandle)
                    else
                        Toast.makeText(this, "Failed to leave event!", Toast.LENGTH_LONG).show()
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }
}


