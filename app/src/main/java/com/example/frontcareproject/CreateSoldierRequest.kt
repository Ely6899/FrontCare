package com.example.frontcareproject

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import utils.GlobalVar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.internal.notify
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CreateSoldierRequest : AppCompatActivity() {
    private lateinit var productsSpinnerView : Spinner
    private lateinit var quantityEditView : EditText
    private lateinit var addProductButton : Button
    private lateinit var createButton : Button
    private lateinit var productsTableView : TableLayout
    private lateinit var radioGroup : RadioGroup

    private var availableProducts : MutableMap<String, String> = mutableMapOf() // create new map to save available products and their ids
    private var adapterList : MutableList<String> = mutableListOf()
    private lateinit var adapter : ArrayAdapter<String>
    private var productsToSend : MutableMap<Int, Int> = mutableMapOf() // Products in the table


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_soldier_request)

        productsSpinnerView = findViewById(R.id.productsSpinner)
        quantityEditView = findViewById(R.id.quantity)
        addProductButton = findViewById(R.id.addToListButton)
        createButton = findViewById(R.id.createButton)
        productsTableView = findViewById(R.id.productsTable)
        radioGroup = findViewById(R.id.radioGroup)

        // Bind spinner adapter to its list
        adapter = ArrayAdapter(
            this@CreateSoldierRequest,
            android.R.layout.simple_spinner_item,
            adapterList
        )

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        productsSpinnerView.adapter = adapter

        // Get availableProducts from server and update spinner
        Thread {
            getProducts()
        }.start()

        // Add button even listener
        addProductButton.setOnClickListener {
            onAddProductButtonClick()
        }

        // Create button event listener
        createButton.setOnClickListener{
            onCreateButtonClick()
        }

        //making the actionBar functional:
        //making the back icon have a back functionality:
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, UserPostings::class.java))
        }
        //making the home icon to have a back to profile functionality:
        val homeIcon = findViewById<ImageView>(R.id.home_icon)
        homeIcon.setOnClickListener {
            GlobalVar.navigateToPage(Intent(this, Profile::class.java))
        }
        // Set the callback
        GlobalVar.navigateCallback = { intent ->
            startActivity(intent)
            finish()
        }
    }

    private fun getProducts() {
        val gson = Gson()
        val type = object : TypeToken<MutableMap<String, String>>() {}.type

        val url = URL("http://${GlobalVar.serverIP}:8080/api/products")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connect()

        // Read the response
        val inputStream = connection.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream))
        val response = reader.readLine()

        runOnUiThread {
            // transform json back to map
            availableProducts = gson.fromJson(response, type)
            adapterList = availableProducts.values.toMutableList() // take only the names of the products.
            adapter.addAll(adapterList) // Add items to spinner
            adapter.notifyDataSetChanged()
        }

        reader.close()
        connection.disconnect()
    }

    private fun onCreateButtonClick() {
        // Check if no location is picked.
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if products list is empty
        if (productsToSend.isEmpty()) {
            Toast.makeText(this, "Please add products to your list", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://${GlobalVar.serverIP}:8080/api/createSoldierRequest"

        // Find the radio button by ID
        val selectedRadioButton: RadioButton = findViewById(selectedRadioButtonId)

        // Get the text of the selected radio button
        val location = selectedRadioButton.text.toString()

        // convert Products map to jsonArray
        val gson = Gson()
        val jsonProducts = gson.toJson(productsToSend)

        // Request in a new Coroutine that is destroyed after leaving this scope
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            // create request body
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val json = """{"userId": ${GlobalVar.userId}, "location": "$location", "products": $jsonProducts}"""
            val requestBody = json.toRequestBody(jsonMediaType)

            // build the request
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            // send request and put response in variable
            val response = client.newCall(request).execute()

            // check response code
            if (response.isSuccessful) {
                // return to requests page activity
                val intent = Intent(this@CreateSoldierRequest, UserPostings::class.java)
                startActivity(intent)
            } else {
                println("Request failed with code: ${response.code}")
            }
        }
    }

    private fun onAddProductButtonClick() {
        // Check if spinner is empty
        if (adapterList.isEmpty()) {
            Toast.makeText(this, "There are no more items to add", Toast.LENGTH_SHORT).show()
            return
        }

        // Get string inputs
        val productName = productsSpinnerView.selectedItem.toString()
        val quantityString = quantityEditView.text.toString()

        // check if quantity input is empty
        if (quantityString.isEmpty()) {
            Toast.makeText(this, "Quantity is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // parse to Int
        val quantityInt = quantityString.toInt()

        // Now check valid input
        if (quantityInt < 1 || quantityInt > 100000) {
            Toast.makeText(this, "Quantity should be between 1 and 100000", Toast.LENGTH_SHORT).show()
            return
        }

        // Find product ID by name.
        val productId = availableProducts.entries.find { it.value == productName }?.key?.toInt()

        // Put in map of table products
        if (productId != null)
            productsToSend[productId] = quantityInt

        // Remove product from the adapter list and update
        adapter.remove(productName)
        adapterList.remove(productName)
        adapter.notifyDataSetChanged()

        // create new table row
        val newRow = TableRow(this)

        // Create text views
        val productView = createTextView(productName)
        val quantityView = createTextView("$quantityInt")

        // Add text views to the row
        newRow.addView(productView)
        newRow.addView(quantityView)

        // Add the row to the table
        productsTableView.addView(newRow)

        // Clear edit texts
        quantityEditView.text.clear()
    }

    /**
     *  Gets a string and returns a TextView object with a fixed style
     */
    private fun createTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setPadding(8, 8, 8, 8)

        // Make the text views take up the whole table
        val newLayoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f)
        newLayoutParams.gravity = Gravity.CENTER

        textView.layoutParams = newLayoutParams

        return textView
    }
}