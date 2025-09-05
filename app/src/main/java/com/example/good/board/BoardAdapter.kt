package com.example.good.board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
class BoardAdapter(
    private var posts: List<Post>,
    private val onClick: (Post) -> Unit
) : RecyclerView.Adapter<BoardAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.postTitle)
        val categoryText: TextView = itemView.findViewById(R.id.postCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.titleText.text = post.title
        holder.categoryText.text = post.category

        // 별점 관련 뷰
        val ratingLayout = holder.itemView.findViewById<LinearLayout>(R.id.ratingLayout)
        val ratingBar = holder.itemView.findViewById<RatingBar>(R.id.ratingBar)
        val ratingText = holder.itemView.findViewById<TextView>(R.id.ratingText)

        if (post.category == "별점" && post.rating != null) {
            ratingLayout.visibility = View.VISIBLE
            ratingBar.rating = post.rating.toFloat()
            ratingText.text = "${post.rating}점"
        } else {
            ratingLayout.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(post) }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}