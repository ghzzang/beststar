package com.example.good.notice

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.good.R

class NoticeDetailFragment : Fragment(R.layout.fragment_profile_notice_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("title") ?: ""
        val date = arguments?.getString("date") ?: ""
        val content = arguments?.getString("content") ?: ""

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "공지사항"
        }

        view.findViewById<TextView>(R.id.titleTextView).text = title
        view.findViewById<TextView>(R.id.dateTextView).text = date
        view.findViewById<TextView>(R.id.contentTextView).text = content

    }
}