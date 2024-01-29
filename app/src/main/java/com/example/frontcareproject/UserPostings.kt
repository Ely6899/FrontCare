package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
    private lateinit var createRequestButton: TextView
    private lateinit var postingsTable: TableLayout
    private lateinit var jsonArray: JSONArray


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_postings)

        postingsTitle = findViewById(R.id.tvPostingsTitle)
        postingsTable = findViewById(R.id.postingsTable)
        nameColumn = findViewById(R.id.tvNameColumn)
        statusColumn = findViewById(R.id.tvStatusColumn)
        dateColumn = findViewById(R.id.tvDateColumn)
        createRequestButton = findViewById(R.id.createRequestButton)

        createRequestButton.setOnClickListener{
            val intent = Intent(this@UserPostings, CreateSoldierRequest::class.java)
            startActivity(intent)
        }

        if (GlobalVar.userType == 1) { //Handle soldier requests
            postingsTitle.text = getString(R.string.requests_history_button)
            nameColumn.text = "From"
            fetchHistory("soldierRequestHistory")
        }else{ //Handle donor donations
            postingsTitle.text = getString(R.string.donations_history_button)
            nameColumn.text = "To"
            fetchHistory("donorDonationHistory")
        }
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
                jsonArray = JSONArray(serverAns)

                runOnUiThread {
                    handleHistoryResponse()
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleHistoryResponse() {
        //Iterate through elements of the answer representing rows
        for (i in 0 until jsonArray.length()){
            val jsonObject = jsonArray.getJSONObject(i)
            addRowToTable(jsonObject)
        }
    }

    private fun addRowToTable(jsonObject: JSONObject) {
        val requestId = jsonObject.getString("request_id")
        val existingRow = postingsTable.findViewWithTag<TableRow>(requestId)

        if(existingRow == null){
            // Create a new row
            val newRow = TableRow(this)
            newRow.tag = requestId // Set tag to request_id for identification

            // Set gray background for the TableRow
            newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

            //Define options spinner for the row
            val optionsSpinner = Spinner(this,  Spinner.MODE_DROPDOWN)
            ArrayAdapter.createFromResource(
                this,
                R.array.posting_history_options,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                // Specify the layout to use when the list of choices appears.
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner.
                optionsSpinner.adapter = adapter
            }

            optionsSpinner.setSelection(0,false)

            optionsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Check if the position is valid
                    when (position) {
                        0 -> {
                            val filteredArray = (0 until jsonArray.length())
                                .map { jsonArray.getJSONObject(it) }
                                .filter { it.getString("request_id") == requestId }
                                .map {
                                    it.toString()
                                }

                            val detailsIntent = Intent(this@UserPostings, SoldierRequestDetails::class.java)
                            detailsIntent.putStringArrayListExtra("jsonArray", ArrayList(filteredArray))
                            detailsIntent.putExtra("fromHistory", true)

                            startActivity(detailsIntent)
                        }
                        1 -> {
                            val filteredArray = (0 until jsonArray.length())
                                .map { jsonArray.getJSONObject(it) }
                                .filter { it.getString("request_id") == requestId }
                                .map {
                                    it.toString()
                                }

                            val editIntent = Intent(this@UserPostings, EditSoldierRequest::class.java)
                            editIntent.putExtra("request_id", newRow.tag as String)
                            editIntent.putStringArrayListExtra("jsonArray", ArrayList(filteredArray))

                            startActivity(editIntent)
                        }
                        2 -> {
                            handleDonationConfirmation(newRow)
                        }
                    }
                }
            }

            newRow.addView(optionsSpinner)

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
                column.setPadding(8, 8, 8, 8)
                // Set black border for the TextView
                column.setBackgroundResource(R.drawable.tables_outline)
                newRow.addView(column)
            }

            postingsTable.addView(newRow)
        }
    }

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
                    val closeDateField = rowToHandle.getChildAt(4) as? TextView
                    val statusField = rowToHandle.getChildAt(1) as? TextView

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