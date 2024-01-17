package com.example.frontcareproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import ch.qos.logback.core.Context
import org.json.JSONArray
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Array
import java.net.HttpURLConnection
import java.net.URL

class UserPostings : AppCompatActivity() {
    private lateinit var postingsTitle: TextView
    private lateinit var postingList: LinearLayout

    //Used for adding elements to the scrollview
    private lateinit var inflater: LayoutInflater


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_postings)

        postingsTitle = findViewById(R.id.tvPostingsTitle)
        postingList = findViewById(R.id.postingScrollList)
        inflater = applicationContext.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater


        if (GlobalVar.userType == 1) { //Handle soldier requests
            postingsTitle.text = getString(R.string.requests_history_button)
            fetchHistory("soldierRequestHistory")
        }else{ //Handle donor donations
            postingsTitle.text = getString(R.string.donations_history_button)
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

        val requestIdList = mutableMapOf<Int, Int>()

        //Iterate through elements of the answer representing rows
        for (i in 0 until jsonAnswer.length()){
            val rowObject = jsonAnswer.getJSONObject(i)
            val requestId = rowObject.getInt("request_id")

            if(requestIdList.containsKey(requestId)){
                val requiredView = postingList.getChildAt(requestIdList[requestId]!!)
                val products = requiredView.findViewById<TextView>(R.id.tvProductsFill)

                //Concatenate existing string
                ("${rowObject.getString("quantity")}-${rowObject.getString("product_name")} " + products.text).also { products.text = it }
            }
            else{
                val newText = inflater.inflate(R.layout.posting_row, null)
                requestIdList[requestId] = i
                val nameView = newText.findViewById<TextView>(R.id.tvNameElement)
                val name = newText.findViewById<TextView>(R.id.tvNameFill)
                val date = newText.findViewById<TextView>(R.id.tvDateFill)
                val status = newText.findViewById<TextView>(R.id.tvStatusFill)
                val closeDate = newText.findViewById<TextView>(R.id.tvCloseDateFill)
                val products = newText.findViewById<TextView>(R.id.tvProductsFill)
                "${rowObject.getString("firstname")} ${rowObject.getString("lastname")}".also { name.text = it }
                date.text = rowObject.getString("request_date")
                if (GlobalVar.userType == 0){
                    nameView.text = "Donated to:"
                    status.visibility = View.GONE
                }
                else{
                    nameView.text = "Donated from:"
                    status.text = rowObject.getString("status")
                }

                closeDate.text = rowObject.getString("close_date")
                "${rowObject.getString("quantity")}-${rowObject.getString("product_name")}".also { products.text = it }
                postingList.addView(newText, i)
            }
        }
    }
}