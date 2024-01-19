package com.example.frontcareproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UserEvents : AppCompatActivity() {

    private lateinit var eventsList: LinearLayout

    //Used for adding elements to the scrollview
    private lateinit var inflater: LayoutInflater


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_events)

        eventsList = findViewById(R.id.eventsScrollList)
        inflater = applicationContext.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater

        if(GlobalVar.userType == 1)
            fetchHistory("soldierEventsHistory")
        else
            fetchHistory("donorEventsHistory")
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
                    if(GlobalVar.userType == 1){
                        handleSoldierHistoryResponse(serverAns)
                    }
                    else{
                        handleDonorHistoryResponse(serverAns)
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

    private fun handleSoldierHistoryResponse(serverAns: String) {
        val jsonAnswer = JSONArray(serverAns)

        //Iterate through elements of the answer representing rows
        for (i in 0 until jsonAnswer.length()){
            val rowObject = jsonAnswer.getJSONObject(i)

            val newText = inflater.inflate(R.layout.event_row, null)
            val name = newText.findViewById<TextView>(R.id.tvNameFill)
            val date = newText.findViewById<TextView>(R.id.tvDateFill)
            val location = newText.findViewById<TextView>(R.id.tvLocationElement)
            val address = newText.findViewById<TextView>(R.id.tvAddressFill)

            val remainingSpotsView = newText.findViewById<TextView>(R.id.tvRemainingElement)
            val remainingSpots = newText.findViewById<TextView>(R.id.tvRemainingFill)

            val productsView = newText.findViewById<TextView>(R.id.tvProductsElement)
            val products = newText.findViewById<TextView>(R.id.tvProductsFill)

            remainingSpotsView.visibility = View.GONE
            remainingSpots.visibility = View.GONE
            productsView.visibility = View.GONE
            products.visibility = View.GONE


            "${rowObject.optString("firstname")} ${rowObject.optString("lastname")}".also { name.text = it }
            date.text = rowObject.optString("event_date")
            location.text = rowObject.optString("event_location")
            address.text = rowObject.optString("event_address")
            eventsList.addView(newText, i)

        }
    }
    private fun handleDonorHistoryResponse(serverAns: String) {
        val jsonAnswer = JSONArray(serverAns)

        val requestIdList = mutableMapOf<Int, Int>()

        //Iterate through elements of the answer representing rows
        for (i in 0 until jsonAnswer.length()){
            val rowObject = jsonAnswer.getJSONObject(i)
            val eventId = rowObject.getInt("event_id")

            if(requestIdList.containsKey(eventId)){
                val requiredView = eventsList.getChildAt(requestIdList[eventId]!!)
                val products = requiredView.findViewById<TextView>(R.id.tvProductsFill)

                //Concatenate existing string
                ("${rowObject.getString("product_name")} " + products.text).also { products.text = it }
            }
            else{
                val newText = inflater.inflate(R.layout.event_row, null)
                requestIdList[eventId] = i
                val nameView = newText.findViewById<TextView>(R.id.tvOrganizerElement)
                val name = newText.findViewById<TextView>(R.id.tvNameFill)
                val date = newText.findViewById<TextView>(R.id.tvDateFill)
                val location = newText.findViewById<TextView>(R.id.tvLocationFill)
                val address = newText.findViewById<TextView>(R.id.tvAddressFill)
                val remainingSpots = newText.findViewById<TextView>(R.id.tvRemainingFill)
                val products = newText.findViewById<TextView>(R.id.tvProductsFill)

                nameView.text = ""
                nameView.visibility = View.GONE
                name.visibility = View.GONE

                date.text = rowObject.optString("event_date")
                location.text = rowObject.optString("event_location")
                address.text = rowObject.optString("event_address")
                remainingSpots.text = rowObject.getInt("remaining_spot").toString()

                rowObject.getString("product_name").also { products.text = it }
                eventsList.addView(newText, i)
            }
        }
    }
}


