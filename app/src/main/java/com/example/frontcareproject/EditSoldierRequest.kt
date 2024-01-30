package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.core.view.forEach
import org.json.JSONArray
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class EditSoldierRequest : AppCompatActivity() {
    private lateinit var addItemBtn: Button
    private lateinit var btnConfirmEdit: Button
    private lateinit var btnRemoveRequest: Button

    private lateinit var itemTable: TableLayout
    private lateinit var spinnerItems: Spinner
    private lateinit var etItemQuantity: EditText
    private lateinit var productsJSON: JSONObject

    //Array of products which will be initialized by DB request
    private lateinit var productsArray: MutableList<String>

    //Map of products contained in a specific request
    private lateinit var productMap: MutableMap<String, Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_soldier_request)

        addItemBtn = findViewById(R.id.btnAddItem)
        btnConfirmEdit = findViewById(R.id.btnConfirmEdit)
        btnRemoveRequest = findViewById(R.id.btnRemoveRequest)
        etItemQuantity = findViewById(R.id.etProductQuantity)
        spinnerItems = findViewById(R.id.itemsSpinner)
        itemTable = findViewById(R.id.itemTable)

        productMap = mutableMapOf()
        productsArray = mutableListOf()

        //Prepare data for inserting as rows to the table.

        val jsonStringList = intent.getStringArrayListExtra("jsonArray")
        val jsonArray = JSONArray()

        if(jsonStringList != null){
            for(jsonString in jsonStringList){
                jsonArray.put(JSONObject(jsonString))
            }
        }

        for (i in 0 until jsonArray.length()){
            val obj = jsonArray.getJSONObject((i))
            productMap[obj.getString("product_name")] = obj.getInt("quantity")
        }

        //Request the products data from the DB
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

                // Iterate over the keys and add their corresponding values to the array
                productsJSON.keys().forEach { key ->
                    val value = productsJSON.getString(key)
                    productsArray.add(value)
                }

                runOnUiThread {
                    //Create spinner with all the products
                    val adapter = ArrayAdapter(this,
                        android.R.layout.simple_spinner_item, productsArray)
                    //adapter.setDropDownViewResource(R.layout.spinner_height_limiter)
                    spinnerItems.adapter = adapter
                    //For each existing product produce a row
                    productMap.keys.forEach {key ->
                        addRowToTable(key, productMap[key].toString())
                    }
                }
                reader.close()
                connection.disconnect()
            } catch (e: IOException) {
                // Handle the exception, e.g., show an error message
                e.printStackTrace()
            }
        }.start()

        addItemBtn.setOnClickListener {
            addRowToTable(spinnerItems.selectedItem.toString(), etItemQuantity.text.toString())
        }

        /*
        TODO(Check the functions below work after they have an API implementation)
        * */

        btnConfirmEdit.setOnClickListener {
            handleEditConfirmation()
        }

        btnRemoveRequest.setOnClickListener {
            handleRequestRemoval()
        }
    }

    private fun handleRequestRemoval() {
        Thread  {
            try {
                //val userId = GlobalVar.userId // Replace with your logic to get the user ID
                val url = URL("http://${GlobalVar.serverIP}:8080/api/removeRequest/${intent.getStringExtra("request_id")}")
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

    private fun handleEditConfirmation() {
        Thread  {
            try {
                //val userId = GlobalVar.userId // Replace with your logic to get the user ID
                val url = URL("http://${GlobalVar.serverIP}:8080/api/updateRequest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connect()

                val newProductsMap = mutableMapOf<String, Int>()
                newProductsMap["request_id"] = intent.getStringExtra("request_id")!!.toInt()

                itemTable.forEach {view ->
                    val currRow = view as TableRow
                    val currSpinner = currRow.getChildAt(0) as Spinner
                    val currEditText = currRow.getChildAt(1) as EditText
                    if (currRow.id > 0){
                        val itemId = productsArray.indexOf(currSpinner.selectedItem.toString()).toString()
                        val itemQuantity = currEditText.text.toString().toInt()
                        if(newProductsMap.containsKey(itemId)){
                            newProductsMap[itemId] = newProductsMap[itemId]!! + itemQuantity
                        }
                        else{
                            newProductsMap[itemId] = itemQuantity
                        }
                    }
                }

                val jsonInputString = JSONObject((newProductsMap as Map<*, *>?)!!).toString().trimIndent()

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

    private fun checkEditRequest(serverAns: String) {
        val jsonResponse = JSONObject(serverAns)
        if (jsonResponse.optString("message") == "Update successfully"){
            startActivity(Intent(this@EditSoldierRequest, UserPostings::class.java))
        }
        else
            Toast.makeText(this," Failed to update request", Toast.LENGTH_LONG).show()
    }



    private fun addRowToTable(product: String, quantity: String){
        // Create a new row
        val newRow = TableRow(this)

        // Set gray background for the TableRow
        newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

        val productSpinnerColumn = Spinner(this,  Spinner.MODE_DROPDOWN)
        val adapterColumn = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, productsArray)
        //adapterColumn.setDropDownViewResource(R.layout.spinner_height_limiter)
        productSpinnerColumn.adapter = adapterColumn

        //Set initial location of item in the spinner
        productSpinnerColumn.setSelection(productsArray.indexOf(product))

        newRow.addView(productSpinnerColumn)

        val etQuantityColumn = EditText(this)
        etQuantityColumn.inputType=InputType.TYPE_CLASS_NUMBER
        etQuantityColumn.setText(quantity)
        etQuantityColumn.textAlignment=EditText.TEXT_ALIGNMENT_CENTER

        newRow.addView(etQuantityColumn)

        val removeItemBtn = Button(this)
        removeItemBtn.text = "-"
        removeItemBtn.setOnClickListener {
            itemTable.removeView(newRow)
        }

        newRow.addView(removeItemBtn)

        itemTable.addView(newRow)
    }
}