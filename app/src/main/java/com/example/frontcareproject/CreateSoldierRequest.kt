package com.example.frontcareproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import server.userId
import utils.GlobalVar

class CreateSoldierRequest : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_soldier_request)

        val productName : TextView = findViewById(R.id.productName);
        val addProductButton : Button = findViewById(R.id.addToListButton);
        val createButton : Button = findViewById(R.id.createButton);
        val productsListView : ListView = findViewById(R.id.productList);

        val productList = ArrayList<String>();
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, productList);
        productsListView.adapter = adapter;

        addProductButton.setOnClickListener {
            val product = productName.text.toString().trim();

                if (product.isNotEmpty()) {
                    productList.add(product);
                    adapter.notifyDataSetChanged();
                productName.text = "";
            }
        }

//        createButton.setOnClickListener{
//            val url = "http://${GlobalVar.serverIP}:8080/api/"
//
//            // Request in a new Coroutine that is destroyed after leaving this scope
//            lifecycleScope.launch(Dispatchers.IO) {
//                val client = OkHttpClient()
//
//                // create request body
//                val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
//                val json = """{"userId": "$userId", "products": "$requestId"}"""
//                val requestBody = json.toRequestBody(jsonMediaType)
//
//                // build the request
//                val request = Request.Builder()
//                    .url(url)
//                    .post(requestBody)
//                    .build()
//
//                // send request and put response in variable
//                val response = client.newCall(request).execute()
//
//                // check response code
//                if (response.isSuccessful) {
//                    // return to requests page activity
//                    val intent = Intent(this@SoldierRequestDetails, SoldiersRequestsPage::class.java)
//                    startActivity(intent)
//                } else {
//                    println("Request failed with code: ${response.code}")
//                }
//            }
//        }
    }
}