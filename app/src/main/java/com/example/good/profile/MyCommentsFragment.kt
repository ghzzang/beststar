package com.example.good.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
import com.example.good.board.BoardComment
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class MyCommentsFragment : Fragment(R.layout.fragment_profile_mycomment) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyCommentsAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var myComments = listOf<BoardComment>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.myCommentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MyCommentsAdapter(emptyList()) { comment -> openCommentDetail(comment) }
        recyclerView.adapter = adapter

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "내가 쓴 댓글"
        }

        loadMyComments()
    }

    private fun loadMyComments() {
        val currentUid = auth.currentUser?.uid ?: return

        db.collection("posts").get().addOnSuccessListener { postsSnapshot ->
            val tempComments = mutableListOf<BoardComment>()
            val tasks = mutableListOf<Task<QuerySnapshot>>()

            for (postDoc in postsSnapshot.documents) {
                val postId = postDoc.id
                val postTitle = postDoc.getString("title") ?: "제목 없음"

                val task = db.collection("posts").document(postId)
                    .collection("comments")
                    .whereEqualTo("authorUid", currentUid)
                    .get()
                tasks.add(task)
            }

            Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                .addOnSuccessListener { results ->
                    for ((index, snapshot) in results.withIndex()) {
                        val title = postsSnapshot.documents[index].getString("title") ?: "제목 없음"
                        for (doc in snapshot.documents) {
                            tempComments.add(
                                BoardComment(
                                    id = doc.id,
                                    postId = doc.reference.parent.parent?.id ?: "",
                                    postTitle = title,
                                    authorUid = doc.getString("authorUid"),
                                    authorNickname = doc.getString("authorNickname"),
                                    nickname = doc.getString("nickname") ?: "사용자",
                                    content = doc.getString("content") ?: "",
                                    timestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                                        ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    myComments = tempComments.sortedByDescending { it.timestamp }
                    adapter.updateComments(myComments)
                }
        }
    }

    private fun openCommentDetail(comment: BoardComment) {
        val fragment = com.example.good.board.BoardDetailFragment()
        val bundle = Bundle().apply {
            putString("postId", comment.postId)
            putString("type", "normal")
            putString("authorUid", comment.authorUid)
        }
        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}