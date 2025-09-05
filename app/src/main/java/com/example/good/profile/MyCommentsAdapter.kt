package com.example.good.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
import com.example.good.board.BoardComment

class MyCommentsAdapter(
    private var comments: List<BoardComment>,
    private val onClick: (BoardComment) -> Unit
) : RecyclerView.Adapter<MyCommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentText: TextView = itemView.findViewById(R.id.commentContent)
        val postTitleText: TextView = itemView.findViewById(R.id.commentPostTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.contentText.text = comment.content
        holder.postTitleText.text = comment.postTitle
        holder.itemView.setOnClickListener { onClick(comment) }
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<BoardComment>) {
        comments = newComments
        notifyDataSetChanged()
    }
}