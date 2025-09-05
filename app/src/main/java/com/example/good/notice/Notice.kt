package com.example.good.notice

import com.google.firebase.Timestamp
data class Notice(
    val title: String = "",
    val content: String = "",
    val date: Timestamp? = null
)