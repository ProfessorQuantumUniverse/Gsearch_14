package com.olafsapp.gsearch14

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class SearchHistoryAdapter(
    private var historyList: MutableList<SearchHistoryItem>,
    private val onItemClick: (SearchHistoryItem) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.historyItemCard)
        val queryText: TextView = itemView.findViewById(R.id.queryText)
        val searchTypeText: TextView = itemView.findViewById(R.id.searchTypeText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val searchTypeIcon: ImageView = itemView.findViewById(R.id.searchTypeIcon)
        val aiIcon: ImageView = itemView.findViewById(R.id.aiIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]

        // Daten setzen
        holder.queryText.text = item.query
        holder.searchTypeText.text = item.searchType

        // Zeitstempel formatieren
        val date = Date(item.timestamp)
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.timestampText.text = formatter.format(date)

        // Icon basierend auf Suchtyp setzen
        val iconRes = when (item.searchType) {
            "Images" -> R.drawable.ic_image
            "Videos" -> R.drawable.ic_video
            "News" -> R.drawable.ic_news
            else -> R.drawable.ic_search
        }
        holder.searchTypeIcon.setImageResource(iconRes)

        // AI-Icon anzeigen/verstecken
        holder.aiIcon.visibility = if (item.useAI) View.VISIBLE else View.GONE

        // Item-Animation beim Laden
        holder.card.apply {
            alpha = 0f
            translationX = 100f
            animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(300)
                .setStartDelay(position * 50L) // Gestaffelter Eintritt
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Click-Handler mit Animation
        holder.card.setOnClickListener { view ->
            // Klick-Animation
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            onItemClick(item)
                        }
                        .start()
                }
                .start()
        }

        // Hover-Effekt (elevation animation)
        holder.card.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .translationZ(8f)
                        .setDuration(150)
                        .start()
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .translationZ(0f)
                        .setDuration(150)
                        .start()
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateHistory(newHistory: MutableList<SearchHistoryItem>) {
        historyList.clear()
        historyList.addAll(newHistory)
        notifyDataSetChanged()
    }
}
