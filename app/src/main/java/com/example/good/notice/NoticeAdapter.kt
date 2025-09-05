package com.example.good.notice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
import java.text.SimpleDateFormat
import java.util.Locale

class NoticeAdapter(private val notices: List<Notice>) :
    RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder>() {

    inner class NoticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.noticeTitle)
        val dateText: TextView = itemView.findViewById(R.id.noticeDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_profile_notice_item, parent, false)
        return NoticeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        val notice = notices[position]

        // 날짜 포맷
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = notice.date?.toDate()?.let { sdf.format(it) } ?: ""

        holder.titleText.text = notice.title
        holder.dateText.text = formattedDate

        holder.itemView.setOnClickListener {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = notice.date?.toDate()?.let { sdf.format(it) } ?: ""

            val fragment = NoticeDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("title", notice.title)
                    putString("content", notice.content)
                    putString("date", formattedDate)
                }
            }

            val activity = holder.itemView.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // BackStack에 추가 → 뒤로가기 가능
                .commit()
        }
    }

    override fun getItemCount(): Int = notices.size
}