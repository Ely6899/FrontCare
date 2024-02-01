package com.example.frontcareproject

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
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

class UserPostings : AppCompatActivity() {
    private lateinit var postingsTitle: TextView
    private lateinit var statusColumn: TextView
    private lateinit var dateColumn: TextView
    private lateinit var nameColumn: TextView
    private lateinit var createRequestButton: Button

    private lateinit var postingsTable: TableLayout

    //Holds all the user's requests/donations
    private lateinit var userPostings: JSONArray

    private lateinit var optionsArray: Array<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_postings)

        postingsTitle = findViewById(R.id.tvPostingsTitle)
        postingsTable = findViewById(R.id.postingsTable)
        nameColumn = findViewById(R.id.tvNameColumn)
        statusColumn = findViewById(R.id.tvStatusColumn)
        dateColumn = findViewById(R.id.tvDateColumn)
        createRequestButton = findViewById(R.id.createRequestButton)

        //Used for creating lists of options for each row
        optionsArray = arrayOf("Details", "Edit", "Confirm")

        // Go to create request page
        createRequestButton.setOnClickListener{
            val intent = Intent(this@UserPostings, CreateSoldierRequest::class.java)
            startActivity(intent)
        }

        if (GlobalVar.userType == 1) { //Handle soldier requests
            postingsTitle.text = getString(R.string.requests_history_button)
            nameColumn.text = getString(R.string.from_donor_column)
            fetchHistory("soldierRequestHistory")
        }else{ //Handle donor donations
            postingsTitle.text = getString(R.string.donations_history_button)
            nameColumn.text = getString(R.string.to_soldier_column)
            fetchHistory("donorDonationHistory")
        }
    }

    //Get all postings history of current user connected.
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
                userPostings = JSONArray(serverAns)

                runOnUiThread {
                    //Iterate through elements of the answer representing rows
                    for (i in 0 until userPostings.length()){
                        val jsonObject = userPostings.getJSONObject(i)
                        addRowToTable(jsonObject)
                    }
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception
                e.printStackTrace()
            }
        }.start()
    }

    private fun addRowToTable(jsonObject: JSONObject) {
        val requestId = jsonObject.getString("request_id")
        val existingRow = postingsTable.findViewWithTag<TableRow>(requestId)

        if(existingRow == null){ // Create a new row
            val newRow = TableRow(this)
            newRow.tag = requestId // Set tag to request_id for identification

            // Set gray background for the TableRow
            newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

            if(GlobalVar.userType == 0){ //Donor

                //Add dummyView in case of a donor in order to keep insertion order of elements
                val dummyView = TextView(this)
                newRow.addView(dummyView)

                //Collapse unnecessary column
                postingsTable.setColumnCollapsed(0, true)
            }
            else{ //Soldier
                val optionsList = ListView(this)

                //Different adapter depending on request status
                val adapter = if (jsonObject.optString("status") != "closed"){
                    ArrayAdapter(this, R.layout.list_item, R.id.text_view, optionsArray)
                }else{
                    ArrayAdapter(this, R.layout.list_item, R.id.text_view, optionsArray.slice(IntRange(0,0)))
                }

                optionsList.adapter = adapter
                optionsList.setBackgroundResource(R.drawable.tables_outline)

                optionsList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    val filteredArray = (0 until userPostings.length())
                        .map { userPostings.getJSONObject(it) }
                        .filter { it.getString("request_id") == newRow.tag.toString() }
                        .map {
                            it.toString()
                        }

                    // Handle item clicks
                    when(optionsArray[position]){
                        "Details" -> {
                            val detailsIntent = Intent(this@UserPostings, SoldierRequestDetails::class.java)
                            detailsIntent.putStringArrayListExtra("jsonArray", ArrayList(filteredArray))
                            detailsIntent.putExtra("fromHistory", true)
                            startActivity(detailsIntent)
                        }
                        "Edit" -> {
                            val editIntent = Intent(this@UserPostings, EditSoldierRequest::class.java)
                            editIntent.putExtra("request_id", newRow.tag.toString())
                            editIntent.putStringArrayListExtra("jsonArray", ArrayList(filteredArray))
                            startActivity(editIntent)
                        }
                        "Confirm" -> {
                            handleDonationConfirmation(newRow)
                        }
                    }
                }
                newRow.addView(optionsList)
            }

            //Holds relevant column data
            val columns = listOf(
                jsonObject.getString("status"),
                jsonObject.getString("request_date"),
                jsonObject.getString("firstname"),
                jsonObject.getString("close_date"))

            // Add columns for each piece of information
            for (columnData in columns) {
                val column = TextView(this)
                column.text = columnData
                column.gravity = android.view.Gravity.CENTER
                column.setPadding(6, 6, 6, 6)
                column.setTextColor(Color.BLACK)
                // Set black border for the TextView
                column.setBackgroundResource(R.drawable.tables_outline)
                newRow.addView(column)
            }
            //Add entire row to the postings table
            postingsTable.addView(newRow)
        }
    }

    //Handles the row which is confirmed.
    private fun handleDonationConfirmation(rowToHandle: TableRow) {
        Thread  {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/donationConfirmation")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Construct the JSON payload with email and password
                val jsonInputString = """{"userId": "${GlobalVar.userId}", "requestId": "${rowToHandle.tag}"}"""

                // Send JSON as the request body
                val outputStream = connection.outputStream
                outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
                outputStream.close()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverAns = reader.readLine()

                runOnUiThread {
                    val jsonNewData = JSONObject(serverAns)
                    val optionsList = rowToHandle.getChildAt(0) as? ListView
                    val statusField = rowToHandle.getChildAt(1) as? TextView
                    val closeDateField = rowToHandle.getChildAt(4) as? TextView

                    if (optionsList != null) {
                        optionsList.adapter = ArrayAdapter(this, R.layout.list_item, R.id.text_view, optionsArray.slice(IntRange(0,0)))
                    }

                    //Sets relevant TextView fields after confirming donation.
                    closeDateField!!.text = jsonNewData.optString("close_date")
                    statusField!!.text = jsonNewData.optString("status")
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