package com.example.frontcareproject

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
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

class CreateSoldierRequest : AppCompatActivity() {
    private lateinit var productsSpinnerView : Spinner
    private lateinit var  quantityEditView : EditText
    private lateinit var  addProductButton : Button
    private lateinit var  createButton : Button
    private lateinit var  productsTableView : TableLayout
    private lateinit var  radioGroup : RadioGroup

    private var availableProducts : MutableMap<String, String> = mutableMapOf() // create new map to save table entries
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

        // Get availableProducts from server
        getProducts()

        // Add button even listener
        addProductButton.setOnClickListener {
            onAddProductButtonClick()
        }

        // Create button event listener
        createButton.setOnClickListener{
            onCreateButtonClick()
        }
    }

    private fun getProducts() {
        val url = "http://${GlobalVar.serverIP}:8080/api/products"

        //var products: MutableMap<String, String> = mutableMapOf()
        val gson = Gson()
        val type = object : TypeToken<MutableMap<String, String>>() {}.type

        // Request in a new Coroutine that is destroyed after leaving this scope
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            // build the request
            val request = Request.Builder()
                .url(url)
                .build()

            // send request and put response in variable
            val response = client.newCall(request).execute()

            // check response code
            if (response.isSuccessful) {
                // need to run on new thread because we are changing the UI
                runOnUiThread {
                    // transform json back to map
                    availableProducts = gson.fromJson(response.body?.string(), type)

                    // use adapter to put the items in the spinner
                    val adapter = ArrayAdapter(
                        this@CreateSoldierRequest,
                        android.R.layout.simple_spinner_item,
                        availableProducts.values.toList() // take only the names of the products.
                    )

                    // Specify the layout to use when the list of choices appears
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                    // Apply the adapter to the spinner
                    productsSpinnerView.adapter = adapter
                }
            } else {
                println("Request failed with code: ${response.code}")
            }
        }
    }

    private fun onCreateButtonClick() {
        // If none radio button is selected return
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) {
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
                val intent = Intent(this@CreateSoldierRequest, Profile::class.java)
                startActivity(intent)
            } else {
                println("Request failed with code: ${response.code}")
            }
        }
    }

    private fun onAddProductButtonClick() {
        val productName = productsSpinnerView.selectedItem.toString()
        val quantity = quantityEditView.text.toString().toInt()

        if (productName.isNotEmpty() && quantity >= 0) {
            // find product ID by name.
            val productId = availableProducts.entries.find { it.value == productName }?.key?.toInt()

            // Put in map
            if (productId != null)
                productsToSend[productId] = quantity

            // create new table row
            val newRow = TableRow(this)

            // Create text views
            val productView = createTextView(productName)
            val quantityView = createTextView("$quantity")

            // Add text views to the row
            newRow.addView(productView)
            newRow.addView(quantityView)

            // Add the row to the table
            productsTableView.addView(newRow)

            // Clear edit texts
            quantityEditView.text.clear()
        }
    }

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