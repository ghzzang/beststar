package com.example.good.board

data class BoardComment(
    val id: String = "",
    val postId: String = "",
    val postTitle: String? = null,
    val authorUid: String? = null,
    val authorNickname: String? = null,
    val nickname: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)