package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Display.Mode
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_postings)

        postingsTitle = findViewById(R.id.tvPostingsTitle)
        postingsTable = findViewById(R.id.postingsTable)
        nameColumn = findViewById(R.id.tvNameColumn)
        statusColumn = findViewById(R.id.tvStatusColumn)
        dateColumn = findViewById(R.id.tvDateColumn)
        createRequestButton = findViewById(R.id.createRequestButton)


        //Collapse button columns on initialization
        //postingsTable.setColumnCollapsed(0, true)

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

                runOnUiThread {
                    handleHistoryResponse(serverAns)
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleHistoryResponse(serverAns: String) {
        val jsonAnswer = JSONArray(serverAns)

        //Iterate through elements of the answer representing rows
        for (i in 0 until jsonAnswer.length()){
            val jsonObject = jsonAnswer.getJSONObject(i)
            addRowToTable(jsonObject)
        }
    }

    private fun addRowToTable(jsonObject: JSONObject) {
        val requestId = jsonObject.getString("request_id")
        val existingRow = postingsTable.findViewWithTag<TableRow>(requestId)

        if (existingRow != null) {
            // If row with the same request_id exists, append product info to the existing row
            appendProductInfoToRow(existingRow, jsonObject.getString("product_name"), jsonObject.getString("quantity"))
        } else {
            // Create a new row
            val newRow = TableRow(this)
            newRow.tag = requestId // Set tag to request_id for identification

            // Set gray background for the TableRow
            newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

            /*
            TODO(Fix bug of listener not working when returning from the screen)
            */

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

            optionsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    //val selection = parent?.getItemAtPosition(position)

                    if(position == 0){ //Details
                        //Nothing YET
                    }

                    if (position == 1){ //Edit
                        val editIntent = Intent(this@UserPostings, EditSoldierRequest::class.java)
                        val productsString = newRow.getChildAt(4) as TextView
                        editIntent.putExtra("products", productsString.text)
                        editIntent.putExtra("request_id", newRow.tag as String)

                        startActivity(editIntent)
                    }

                    if (position == 2){ //Confirm
                        handleDonationConfirmation(newRow)
                    }
                }
            }

            newRow.addView(optionsSpinner)

//            if(GlobalVar.userType == 1){
//                // Reverse button column collapsing
//                postingsTable.setColumnCollapsed(0, false)
//
//                //Define the two buttons of the row
//                val confirmButton = Button(this)
//                confirmButton.text = "Confirm"
//                confirmButton.setOnClickListener {
//                    handleDonationConfirmation(newRow)
//                    confirmButton.isEnabled = false
//                }
//
//                val editRequestButton = Button(this)
//                editRequestButton.text = getString(R.string.edit_button_history_tables)
//                editRequestButton.setOnClickListener {
//                    handleEditRequest()
//                }

//                //Add two buttons to the row
//                newRow.addView(confirmButton)
//                newRow.addView(editRequestButton)
//            }
//            else{
//                newRow.addView(TextView(this))
//                newRow.addView(TextView(this))
//            }

            val columns = listOf(
                jsonObject.getString("status"),
                jsonObject.getString("request_date"),
                jsonObject.getString("firstname"),
                "${jsonObject.getString("product_name")} - ${jsonObject.getString("quantity")}",
                jsonObject.getString("close_date"))

            // Add columns for each piece of information

            for (columnData in columns) {
                val column = TextView(this)
                column.text = columnData
                column.gravity = android.view.Gravity.CENTER
                //column.setPadding(8, 8, 8, 8)
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
                    val closeDateField = rowToHandle.getChildAt(5) as? TextView
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

    private fun appendProductInfoToRow(existingRow: TableRow, productName: String, quantity: String) {
        // Find the column for productName and quantity in the existing row
        val productInfoColumn = existingRow.getChildAt(4) as? TextView

        // Append new product info to the existing column
        productInfoColumn?.text = "${productInfoColumn?.text}, $productName - $quantity"
    }
}