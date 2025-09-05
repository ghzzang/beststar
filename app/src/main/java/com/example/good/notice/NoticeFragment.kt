package com.example.good.notice

import android.os.Bundle
import com.example.good.notice.NoticeAdapter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.util.Locale

class NoticeFragment : Fragment(R.layout.fragment_profile_notice) {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoticeAdapter
    private val noticeList = mutableListOf<Notice>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        // Toolbar
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "공지사항"
            setDisplayHomeAsUpEnabled(true)
        }

        // RecyclerView
        recyclerView = view.findViewById(R.id.noticeRecyclerView)
        adapter = NoticeAdapter(noticeList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        db = FirebaseFirestore.getInstance()
        loadNotices()
    }

    private fun loadNotices() {
        db.collection("notices")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                noticeList.clear()
                for (doc in documents) {
                    val notice = doc.toObject(Notice::class.java)
                    noticeList.add(notice)
                }
                adapter.notifyDataSetChanged()
            }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                parentFragmentManager.popBackStack() // 뒤로가기
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}