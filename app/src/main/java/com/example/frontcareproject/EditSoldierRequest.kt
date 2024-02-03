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
import org.json.JSONArray
import org.json.JSONObject
import utils.GlobalVar
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson

class EditSoldierRequest : AppCompatActivity() {
    private lateinit var addItemBtn: Button
    private lateinit var btnConfirmEdit: Button
    private lateinit var btnRemoveRequest: Button

    private lateinit var itemTable: TableLayout
    private lateinit var spinnerItems: Spinner
    private lateinit var etItemQuantity: EditText


    private lateinit var productsJSON: JSONObject

    //Array of products which will be initialized by DB request
    private lateinit var productsList: MutableList<Pair<String, String>>

    //Map of products contained in a specific request
    private lateinit var requestProductMap: MutableMap<String, Int>

    //Used for remembering which items were before editing. Used for case of item removal in edit.
    private lateinit var itemIndexInitial: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_soldier_request)

        addItemBtn = findViewById(R.id.btnAddItem)
        btnConfirmEdit = findViewById(R.id.btnConfirmEdit)
        btnRemoveRequest = findViewById(R.id.btnRemoveRequest)
        etItemQuantity = findViewById(R.id.etProductQuantity)
        spinnerItems = findViewById(R.id.itemsSpinner)
        itemTable = findViewById(R.id.itemTable)

        requestProductMap = mutableMapOf()
        productsList = mutableListOf()
        itemIndexInitial = mutableListOf()

        //Prepare data for inserting as rows to the table.
        val postingsList = intent.getStringArrayListExtra("jsonArray")
        val postingsJsonArray = JSONArray()

        if(postingsList != null){
            for(jsonString in postingsList){
                postingsJsonArray.put(JSONObject(jsonString))
            }
        }

        //Save the existing request products.
        for (i in 0 until postingsJsonArray.length()){
            val obj = postingsJsonArray.getJSONObject((i))
            requestProductMap[obj.getString("product_name")] = obj.getInt("quantity")
        }

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
                    //Create spinner with all the products
                    val adapter = ArrayAdapter(this,
                        android.R.layout.simple_spinner_item, productsList.map { it.second })
                    spinnerItems.adapter = adapter
                    //For each existing product create a row
                    requestProductMap.keys.forEach { product ->
                        addRowToTable(product, requestProductMap[product].toString())
                    }
                }
                reader.close()
                connection.disconnect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()

        //Button for adding a new product to the list.
        addItemBtn.setOnClickListener {
            if(etItemQuantity.text.toString() != "" && etItemQuantity.text.toString().isNotEmpty() && etItemQuantity.text.toString().toInt() > 0)
                addRowToTable(spinnerItems.selectedItem.toString(), etItemQuantity.text.toString(), false)
            else
                Toast.makeText(this, "Enter more then 1 item", Toast.LENGTH_SHORT).show()
        }

        //Button for triggering the edit process.
        btnConfirmEdit.setOnClickListener {
            val rowCount = itemTable.childCount
            //A map which holds the new set of items of the request.
            val newProductsMap = mutableMapOf<String, String>()
            var validity: Boolean = (rowCount > 1)

            newProductsMap["request_id"] = intent.getStringExtra("request_id").toString()

            if (validity){
                //Iterate through all table rows.
                for (i in 1 until rowCount) {
                    val view = itemTable.getChildAt(i)
                    if (view is TableRow) { //Valid row
                        val currSpinner = view.getChildAt(0) as Spinner
                        val currEditText = view.getChildAt(1) as EditText

                        //Array index
                        val itemIndex = productsList.indexOfFirst { it.second == currSpinner.selectedItem.toString() }

                        //True ID from DB
                        val itemDataId = productsList[itemIndex].first
                        val itemQuantity = currEditText.text.toString()
                        if(currEditText.text.toString() == "" || currEditText.text.toString().isEmpty()){
                            validity = false
                            break
                        }

                        //Handle cumulative item selection
                        if(newProductsMap.containsKey(itemDataId)){ //Handle same type in multiple rows.
                            newProductsMap[itemDataId] = (newProductsMap[itemDataId]!!.toInt() + itemQuantity.toInt()).toString()
                        }
                        else{ //Item appears for the first time
                            newProductsMap[itemDataId] = itemQuantity
                        }
                    }
                }

                //Handle a product which appeared before edit but removed after it.
                itemIndexInitial.forEach { itemDataId->
                    if(!newProductsMap.containsKey(itemDataId)){
                        newProductsMap[itemDataId] = "0"
                    }
                }
            }
            if (validity) //If all checks are valid.
                handleEditConfirmation(newProductsMap)
            else
                Toast.makeText(this, "Enter at least 1 item!", Toast.LENGTH_SHORT).show()
        }

        //Button for removing the request.
        btnRemoveRequest.setOnClickListener {
            handleRequestRemoval()
        }
    }

    //Removes the specific request
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

    // Collects edited data and sends it to the server to update the DB.
    private fun handleEditConfirmation(newProductsMap: MutableMap<String, String>) {
        //Sending the new data to the server.
        Thread  {
            try {
                val url = URL("http://${GlobalVar.serverIP}:8080/api/updateRequest")
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

    //Checks if the edit/remove have passed through the server.
    private fun checkEditRequest(serverAns: String) {
        val jsonResponse = JSONObject(serverAns)
        val responseMsg = jsonResponse.optString("message")
        if (responseMsg == "Update successfully" || responseMsg == "Removed successfully"){
            startActivity(Intent(this@EditSoldierRequest, UserPostings::class.java))
        }
        else
            Toast.makeText(this," Failed to update request", Toast.LENGTH_LONG).show()
    }

    /*
    * Creates new rows to the products table.
    * onCreation parameter is used for saving initial product ID's to itemIndexInitial
    * */
    private fun addRowToTable(product: String, quantity: String, onCreation: Boolean = true){
        // Create a new row
        val newRow = TableRow(this)

        // Set gray background for the TableRow
        newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

        val productSpinnerColumn = Spinner(this,  Spinner.MODE_DROPDOWN)
        val adapterColumn = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, productsList.map { it.second })
        productSpinnerColumn.adapter = adapterColumn

        //Set initial location of item in the spinner
        val itemIndex: Int = productsList.indexOfFirst { it.second == product }
        productSpinnerColumn.setSelection(itemIndex)

        //True ID from the DB
        val productId = productsList[itemIndex].first

        //Add item index to the index memory for later use
        if(onCreation && !itemIndexInitial.contains(productId)){
            itemIndexInitial.add(productId)
        }

        newRow.addView(productSpinnerColumn)

        //Add corresponding quantity field
        val etQuantityColumn = EditText(this)
        etQuantityColumn.inputType=InputType.TYPE_CLASS_NUMBER
        etQuantityColumn.setText(quantity)
        etQuantityColumn.textAlignment=EditText.TEXT_ALIGNMENT_CENTER
        newRow.addView(etQuantityColumn)

        //Add corresponding remove button which removes row.
        val removeItemBtn = Button(this)
        removeItemBtn.text = "-"
        removeItemBtn.setOnClickListener {
            itemTable.removeView(newRow)
        }

        newRow.addView(removeItemBtn)

        itemTable.addView(newRow)
    }
}