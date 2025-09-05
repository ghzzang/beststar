package com.example.good.profile

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.good.R
import com.example.good.board.Post
import com.example.good.board.BoardDetailFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

class MyPostsFragment : Fragment(R.layout.fragment_profile_myposts) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyPostsAdapter
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private var myPosts = listOf<Post>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.myPostsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MyPostsAdapter(emptyList()) { post -> openDetail(post) }
        recyclerView.adapter = adapter

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "내 게시물"
        }

        loadMyPosts()
    }

    private fun loadMyPosts() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Log.e("MyPostsFragment", "로그인된 사용자가 없습니다.")
            return
        }

        db.collection("posts")
            .whereEqualTo("authorUid", currentUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MyPostsFragment", "Firestore 오류: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    Log.e("MyPostsFragment", "Snapshot이 null입니다.")
                    return@addSnapshotListener
                }

                try {
                    myPosts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(
                            id = doc.id,
                            likes = doc.get("likes") as? Map<String, Boolean>
                        )
                    }
                    Log.d("MyPostsFragment", "불러온 게시물 수: ${myPosts.size}")
                    adapter.updatePosts(myPosts)
                } catch (e: Exception) {
                    Log.e("MyPostsFragment", "게시물 변환 중 오류: ${e.message}")
                }
            }
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