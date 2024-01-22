package com.example.frontcareproject

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
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
import com.google.gson.JsonArray
import com.google.protobuf.Internal.MapAdapter

class CreateSoldierRequest : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_soldier_request)

        val productNameEditView : EditText = findViewById(R.id.productName);
        val quantityEditView : EditText = findViewById(R.id.quantity);
        val addProductButton : Button = findViewById(R.id.addToListButton);
        val createButton : Button = findViewById(R.id.createButton);
        val productsTableView : TableLayout = findViewById(R.id.productsTable);
        val radioGroup : RadioGroup = findViewById(R.id.radioGroup);

        // create new map to save table entries
        val products = mutableMapOf<String, Int>();

        // Add button even listener
        addProductButton.setOnClickListener {
            val productName = productNameEditView.text.toString().trim();
            val quantity = quantityEditView.text.toString().toInt()

            if (productName.isNotEmpty() && quantity >= 0) {
                // Put in map
                products[productName] = quantity

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
                productNameEditView.text.clear()
            }
        }

        // Create button event listener
        createButton.setOnClickListener{
            // If none radio button is selected return
            val selectedRadioButtonId = radioGroup.checkedRadioButtonId
            if (selectedRadioButtonId == -1) {
                return@setOnClickListener
            }

            val url = "http://${GlobalVar.serverIP}:8080/api/createSoldierRequest"

            // Find the radio button by ID
            val selectedRadioButton: RadioButton = findViewById(selectedRadioButtonId)

            // Get the text of the selected radio button
            val location = selectedRadioButton.text.toString()

            // convert Products map to jsonArray
            val gson = Gson()
            val jsonProducts = gson.toJson(products)

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