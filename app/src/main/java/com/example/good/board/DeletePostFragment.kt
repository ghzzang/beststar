package com.example.good.board

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.example.good.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class DeletePostFragment : Fragment(R.layout.fragment_delete_post) {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private lateinit var btnDelete: Button
    private var postId: String? = null
    private var postAuthorUid: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnDelete = view.findViewById(R.id.btnDelete)
        postId = arguments?.getString("postId") ?: return

        // 글 작성자 확인
        db.collection("posts").document(postId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    postAuthorUid = doc.getString("authorUid")
                    val currentUid = auth.currentUser?.uid
                    // 작성자가 아니면 버튼 숨기기
                    if (postAuthorUid != currentUid) {
                        btnDelete.visibility = View.GONE
                        Toast.makeText(requireContext(), "본인 글만 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        btnDelete.setOnClickListener {
            deletePost()
        }
    }

    private fun deletePost() {
        postId ?: return
        db.collection("posts").document(postId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}