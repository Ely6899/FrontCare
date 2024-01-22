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

class UserPostings : AppCompatActivity() {
    private lateinit var postingsTitle: TextView
    private lateinit var statusColumn: TextView
    private lateinit var dateColumn: TextView
    private lateinit var nameColumn: TextView
    private lateinit var createRequestButton: TextView

    //private lateinit var postingList: LinearLayout
    private lateinit var postingsTable: TableLayout

    //Used for adding elements to the scrollview
    //private lateinit var inflater: LayoutInflater


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

        //val requestIdList = mutableMapOf<Int, Int>()

        //Iterate through elements of the answer representing rows
        for (i in 0 until jsonAnswer.length()){
            val jsonObject = jsonAnswer.getJSONObject(i)
//            addRowToTable(
//                jsonObject.getString("request_id"),
//                jsonObject.getString("request_date"),
//                jsonObject.getString("firstname"),
//                jsonObject.getString("product_name"),
//                jsonObject.getString("quantity"),
//                jsonObject.getString("close_date"),
//                jsonObject.getString("status"))

            addRowToTable(jsonObject)
        }
    }

    private fun addRowToTable(
//        requestId: String,
//        requestDate: String,
//        firstname: String,
//        productName: String,
//        quantity: String,
//        closeDate: String,
//        status: String
        jsonObject: JSONObject
    ) {
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

            val columns: List<String>

            if(GlobalVar.userType == 1){
                columns = listOf(
                    jsonObject.getString("status"),
                    jsonObject.getString("request_date"),
                    jsonObject.getString("firstname"),
                    "${jsonObject.getString("product_name")} - ${jsonObject.getString("quantity")}",
                    jsonObject.getString("close_date"))

                // Add the button to the last column
                val confirmButton = Button(this)
                confirmButton.text = "Confirm"
                confirmButton.setOnClickListener {
                    handleDonationConfirmation(newRow)
                }
                newRow.addView(confirmButton)
            }
            else{
                columns = listOf(
                    "",
                    "",
                    jsonObject.getString("firstname"),
                    "${jsonObject.getString("product_name")} - ${jsonObject.getString("quantity")}",
                    jsonObject.getString("close_date"))
            }


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

    private fun handleDonationConfirmation(rowToHandle: View) {
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
                    TODO("Handle closing request with a separate function")
                    //fetchHistory("soldierRequestHistory")
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