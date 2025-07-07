package com.olafsapp.gsearch14

data class SearchHistoryItem(
    val query: String,
    val searchType: String,
    val useAI: Boolean,
    val timestamp: Long
)
