package com.example.frontcareproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChecklistAdapter(private val itemList: List<ChecklistItem>) :
    RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_checklist, parent, false)
        return ChecklistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val currentItem = itemList[position]
        holder.textViewItem.text = currentItem.text
        holder.checkBoxItem.isChecked = currentItem.isChecked

        holder.checkBoxItem.setOnCheckedChangeListener { _, isChecked ->
            currentItem.isChecked = isChecked
        }
    }

    override fun getItemCount() = itemList.size

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewItem: TextView = itemView.findViewById(R.id.textViewItem)
        val checkBoxItem: CheckBox = itemView.findViewById(R.id.checkBoxItem)
    }

    // Method to retrieve checked items
    fun getCheckedItems(): List<ChecklistItem> {
        return itemList.filter { it.isChecked }
    }
}
