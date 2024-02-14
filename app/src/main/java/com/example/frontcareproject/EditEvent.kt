package com.example.frontcareproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

//        btnConfirmEdit.setOnClickListener {
//
//        }
//
//        btnRemoveEvent.setOnClickListener {
//
//        }
    }
}