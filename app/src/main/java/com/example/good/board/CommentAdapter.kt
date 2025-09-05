package com.example.good.board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private var comments: List<BoardComment>,
    private val currentUser: String,
    private val postId: String,
    private val db: FirebaseFirestore
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nickname: TextView = itemView.findViewById(R.id.commentNickname)
        val content: TextView = itemView.findViewById(R.id.commentContent)
        val time: TextView = itemView.findViewById(R.id.commentTime)
        val deleteButton: Button = itemView.findViewById(R.id.btnDeleteComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CommentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false))

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.nickname.text = comment.nickname
        holder.content.text = comment.content
        holder.time.text = formatTime(comment.timestamp)

        if (comment.nickname.trim() == currentUser.trim()) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener {
                db.collection("posts").document(postId)
                    .collection("comments").document(comment.id)
                    .delete()
            }
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.deleteButton.setOnClickListener(null)
        }
    }

    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<BoardComment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}