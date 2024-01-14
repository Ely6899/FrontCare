package com.example.frontcareproject

import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SoldiersRequestsPage : AppCompatActivity() {

    //vars:
    private lateinit var donationsTable: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soldiers_requests_page)

        // Find the TableLayout
        donationsTable = findViewById(R.id.donationsTable)

        // Example: Add a row dynamically
        addRowToTable("Dynamic Row, Column 1", "Dynamic Row, Column 2")
        // Add more rows as needed with different data
    }

    private fun addRowToTable(column1Data: String, column2Data: String) {
        val newRow = TableRow(this)

        // Set gray background for the TableRow
        newRow.setBackgroundColor(getColor(R.color.tablesBackgroundColor))

        val column1 = TextView(this)
        column1.text = column1Data
        column1.gravity = android.view.Gravity.CENTER
        column1.setPadding(8, 8, 8, 8)

        // Set black border for the TextView
        column1.setBackgroundResource(R.drawable.tables_outline)

        val column2 = TextView(this)
        column2.text = column2Data
        column2.gravity = android.view.Gravity.CENTER
        column2.setPadding(8, 8, 8, 8)

        // Set black border for the TextView
        column2.setBackgroundResource(R.drawable.tables_outline)

        newRow.addView(column1)
        newRow.addView(column2)

        donationsTable.addView(newRow)
    }
}
