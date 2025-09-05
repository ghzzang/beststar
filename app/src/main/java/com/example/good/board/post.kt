package com.example.good.board

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",                   // 문서 ID
    val title: String = "",
    val content: String = "",
    val authorUid: String? = null,        // 작성자 UID
    val authorNickname: String? = null,   // 작성자 닉네임
    val category: String = "",
    val type: String = "post",            // "review" or "post"
    val timestamp: Timestamp? = null,
    val rating: Int? = null,              // 별점
    val imageUrl: String? = null,
    val likes: Map<String, Boolean>? = null // 좋아요 Map<UID, true>
)