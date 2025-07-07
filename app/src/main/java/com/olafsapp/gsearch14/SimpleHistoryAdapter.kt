package com.olafsapp.gsearch14

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleHistoryAdapter(
    private var historyItems: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SimpleHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val queryText: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyItems[position]
        holder.queryText.text = item
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = historyItems.size

    fun updateItems(newItems: List<String>) {
        historyItems.clear()
        historyItems.addAll(newItems)
        notifyDataSetChanged()
    }
}
