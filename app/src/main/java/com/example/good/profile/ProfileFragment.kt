package com.example.good.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.good.Login.LoginActivity
import com.bumptech.glide.Glide
import com.example.good.notice.NoticeFragment
import com.example.good.R
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var storageRef: StorageReference
    private lateinit var db: FirebaseFirestore


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        profileImageView = view.findViewById(R.id.profileImageView)
        usernameTextView = view.findViewById(R.id.usernameTextView)

        // 프로필 불러오기
        loadUserProfile()


        val logoutButton = view.findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logoutUser()
        }

        val deleteButton = view.findViewById<Button>(R.id.withdrawButton)
        deleteButton.setOnClickListener {
            confirmAndDeleteAccount()
        }
        // 프로필 수정 버튼
        val editProfileButton = view.findViewById<Button>(R.id.editButton)
        editProfileButton.setOnClickListener {
            // Fragment 전환
            val editFragment = EditButtonFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editFragment) // Activity에서 fragment가 들어갈 container ID
                .addToBackStack(null)
                .commit()
        }
        // 공지사항 버튼
        val noticeButton = view.findViewById<Button>(R.id.noticeButton)
        noticeButton?.setOnClickListener {
            val noticeFragment = NoticeFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, noticeFragment) // Activity에서 fragment가 들어갈 container ID
                .addToBackStack(null)
                .commit()
        }

        // Toolbar 제목만 설정 (setSupportActionBar 제거)
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.title = "마이페이지"

        setupButtons(view)

        }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        // Firestore에서 닉네임 불러오기
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "사용자"
                    usernameTextView.text = username
                }
            }
            .addOnFailureListener {
                usernameTextView.text = "사용자"
            }

        // Storage에서 프로필 사진 불러오기
        val profileImageRef = storageRef.child("profileImage/$userId/profile.jpg")
        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(requireContext())
                .load(uri)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(profileImageView)
        }.addOnFailureListener {
            profileImageView.setImageResource(R.drawable.ic_profile)
        }
    }
    private fun setupButtons(view: View) {
        // 작성한 게시물 버튼
        val myPostsButton = view.findViewById<Button>(R.id.myPostsButton)
        myPostsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyPostsFragment())
                .addToBackStack(null)
                .commit()
        }

        // 작성한 댓글 버튼
        val myCommentsButton = view.findViewById<Button>(R.id.mycommentButton)
        myCommentsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyCommentsFragment())
                .addToBackStack(null)
                .commit()
        }
        // 관심 게시물 버튼
        val likedPostsButton = view.findViewById<Button>(R.id.likedpostButton)
        likedPostsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyLikedPostsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun logoutUser() {
        auth.signOut()
        clearAutoLogin()
        goToLoginScreen()
    }

    private fun confirmAndDeleteAccount() {
        // 탈퇴 확인 다이얼로그
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("회원탈퇴")
            .setMessage("정말로 회원탈퇴 하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("탈퇴") { _, _ ->
                deleteAccount()
            }
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                clearAutoLogin()
                goToLoginScreen()
            } else {
                // 탈퇴 실패 시 예외 처리
                // 재인증 필요하면 여기서 처리 가능
            }
        }
    }

    private fun clearAutoLogin() {
        val prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_login", false).apply()
    }

    private fun goToLoginScreen() {
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}