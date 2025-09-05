package com.example.good.board

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.Button
import java.text.SimpleDateFormat
import java.util.Locale

class BoardFragment : Fragment(R.layout.fragment_board) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BoardAdapter
    private lateinit var btnStar: Button
    private lateinit var btnFree: Button
    private lateinit var btnPopular: Button
    private lateinit var btnQnA: Button
    private lateinit var btnPromo: Button
    private lateinit var fabWrite: FloatingActionButton

    private val db = Firebase.firestore
    private var allPosts = listOf<Post>()
    private var currentCategory = "자유"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.boardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BoardAdapter(emptyList()) { post -> openDetail(post) }
        recyclerView.adapter = adapter

        btnStar = view.findViewById(R.id.btnstar)
        btnFree = view.findViewById(R.id.btnFree)
        btnPopular = view.findViewById(R.id.btnPopular)
        btnQnA = view.findViewById(R.id.btnQnA)
        btnPromo = view.findViewById(R.id.btnPromo)
        fabWrite = view.findViewById(R.id.fabWrite)

        // Firestore 실시간 데이터 가져오기
        db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                allPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(
                        id = doc.id,
                        likes = doc.get("likes") as? Map<String, Boolean>
                    )
                }
                filterPosts(currentCategory)
            }

        // 버튼 클릭 + 강조
        btnStar.setOnClickListener { filterPosts("별점"); highlightSelectedButton(btnStar) }
        btnFree.setOnClickListener { filterPosts("자유"); highlightSelectedButton(btnFree) }
        btnPopular.setOnClickListener { filterPosts("인기"); highlightSelectedButton(btnPopular) }
        btnQnA.setOnClickListener { filterPosts("질문과답변"); highlightSelectedButton(btnQnA) }
        btnPromo.setOnClickListener { filterPosts("홍보"); highlightSelectedButton(btnPromo) }

        fabWrite.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WritePostFragment())
                .addToBackStack(null)
                .commit()
        }

        // 초기 선택 강조
        highlightSelectedButton(btnFree)
    }

    private fun filterPosts(category: String) {
        currentCategory = category
        val filtered = when (category) {
            "인기" -> allPosts.filter {
                it.category in listOf("자유", "별점", "질문과답변") &&
                        (it.likes?.values?.count { liked -> liked } ?: 0) > 0
            }.sortedByDescending { it.likes?.values?.count { it } ?: 0 }
            else -> allPosts.filter { it.category == category }
        }
        adapter.updatePosts(filtered)
    }

    private fun highlightSelectedButton(selected: Button) {
        val buttons = listOf(btnStar, btnFree, btnPopular, btnQnA, btnPromo)
        buttons.forEach {
            it.setBackgroundColor(resources.getColor(R.color.white))
            it.setTextColor(resources.getColor(R.color.black))
        }
        selected.setBackgroundColor(resources.getColor(R.color.teal_200))
        selected.setTextColor(resources.getColor(R.color.white))
    }

    private fun openDetail(post: Post) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = post.timestamp?.toDate()?.let { sdf.format(it) } ?: "날짜 없음"

        val detailFragment = BoardDetailFragment()
        val bundle = Bundle().apply {
            putString("title", post.title)
            putString("content", post.content)
            putString("date", dateStr)
            putString("postId", post.id)
            putString("category", post.category)
            putString("type", post.type)
            putString("authorUid", post.authorUid)
        }
        detailFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}