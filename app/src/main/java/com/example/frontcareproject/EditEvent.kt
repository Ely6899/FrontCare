package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class EditEvent : AppCompatActivity() {
    private lateinit var btnConfirmEdit: Button
    private lateinit var btnRemoveEvent: Button

    private lateinit var locationRadioGroup: RadioGroup
    private lateinit var radioNorth: RadioButton
    private lateinit var radioCenter: RadioButton
    private lateinit var radioSouth: RadioButton

    private lateinit var etAddress: EditText

    private lateinit var itemSelectList: RecyclerView
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var itemList: MutableList<ChecklistItem>

    private lateinit var productsJSON: JSONObject

    //Array of products which will be initialized by DB request
    private lateinit var productsList: MutableList<Pair<String, String>>

    //Map of products contained in a specific request
    private lateinit var eventProductList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_event)

        btnConfirmEdit = findViewById(R.id.btnConfirmEventEdit)
        btnRemoveEvent = findViewById(R.id.btnRemoveEvent)

        locationRadioGroup = findViewById(R.id.radioGrpSelectLocation)
        radioNorth = findViewById(R.id.radioNorth)
        radioCenter = findViewById(R.id.radioCenter)
        radioSouth = findViewById(R.id.radioSouth)

        etAddress = findViewById(R.id.etAddress)

        productsList = mutableListOf()
        eventProductList = mutableListOf()
        itemList =  mutableListOf()

        itemSelectList = findViewById(R.id.productsSelectList)

        val eventsList = intent.getStringArrayListExtra("jsonArray")
        val eventsJsonArray = JSONArray()

        if(eventsList != null){
            for(jsonString in eventsList){
                eventsJsonArray.put(JSONObject(jsonString))
            }
        }

        val currLocation = eventsJsonArray.getJSONObject((0)).getString("event_location")
        val currAddress = eventsJsonArray.getJSONObject((0)).getString("event_address")

        //Save the existing request products.
        for (i in 0 until eventsJsonArray.length()){
            val obj = eventsJsonArray.getJSONObject((i))
            eventProductList.add(obj.getString("product_name"))
        }

        when(currLocation){
            "North" -> {
                radioNorth.isChecked = true
            }

            "Center" -> {
                radioCenter.isChecked = true
            }

            "South" -> {
                radioSouth.isChecked = true
            }
        }

        etAddress.setText(currAddress)

        //Request the products data from the DB and create request product table
        Thread  {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/products")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                productsJSON = JSONObject(reader.readLine())

                // Iterate over the keys and add the (id, name) pairs to the list
                productsJSON.keys().forEach { key ->
                    val value = productsJSON.getString(key)
                    productsList.add(Pair(key, value))
                }

                runOnUiThread {
                    for(product in productsList){
                        if(eventProductList.contains(product.second)){
                            itemList.add(ChecklistItem(product.second, true))
                        }
                        else{
                            itemList.add(ChecklistItem(product.second, false))
                        }
                    }
                    itemSelectList.layoutManager = LinearLayoutManager(this)

                    checklistAdapter = ChecklistAdapter(itemList)
                    itemSelectList.adapter = checklistAdapter
                }

                reader.close()
                connection.disconnect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()

        btnConfirmEdit.setOnClickListener {
            val newLocationRadioId: Int = locationRadioGroup.checkedRadioButtonId
            val newAddress: String = etAddress.text.toString()
            val checkedItems: List<String> = checklistAdapter.getCheckedItems().map { it.text }

            if(newLocationRadioId == -1 || newAddress.isEmpty() || checkedItems.isEmpty()){
                Toast.makeText(this, "Invalid edit", Toast.LENGTH_LONG).show()
            }
            else{
                val newLocation = findViewById<RadioButton>(newLocationRadioId).text.toString()

                val newProductsMap = mutableMapOf<String, String>()

                newProductsMap["event_id"] = intent.getStringExtra("event_id").toString()
                newProductsMap["event_location"] = newLocation
                newProductsMap["event_address"] = newAddress



                productsList.forEach { (itemDataIndex, product) ->
                    newProductsMap[itemDataIndex] = if(checkedItems.contains(product)) "1" else "0"
                }
                handleEditConfirmation(newProductsMap)
            }
        }

        btnRemoveEvent.setOnClickListener {
            handleEventRemoval()
        }

        //making the actionBar functional:
        //making the back icon have a back functionality:
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, UserPostings::class.java))
        }
        // Set the callback
        GlobalVar.navigateCallback = { intent ->
            startActivity(intent)
            finish()
        }
    }

    private fun handleEditConfirmation(newProductsMap: MutableMap<String, String>) {
        //Sending the new data to the server.
        Thread  {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/updateEvent")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                val gson = Gson()
                val jsonInputString = gson.toJson(newProductsMap)

                // Send JSON as the request body
                val outputStream = connection.outputStream
                outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
                outputStream.close()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverAns = reader.readLine()

                runOnUiThread {
                    checkEditRequest(serverAns)
                }

                reader.close()
                connection.disconnect()

            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleEventRemoval(){
        Thread{
            try{
                //val userId = GlobalVar.userId // Replace with your logic to get the user ID
                val url = URL("http://${GlobalVar.serverIP}:8080/api/removeEvent/${intent.getStringExtra("event_id")}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                // Read the response
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val serverAns = reader.readLine()

                runOnUiThread {
                    checkEditRequest(serverAns)
                }

                reader.close()
                connection.disconnect()
            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()
    }

    private fun checkEditRequest(serverAns: String) {
        val jsonResponse = JSONObject(serverAns)
        val responseMsg = jsonResponse.optString("message")
        if (responseMsg == "Updated successfully" || responseMsg == "Removed successfully"){
            startActivity(Intent(this@EditEvent, UserEvents::class.java))
        }
        else
            Toast.makeText(this," Failed to update request", Toast.LENGTH_LONG).show()
    }
}