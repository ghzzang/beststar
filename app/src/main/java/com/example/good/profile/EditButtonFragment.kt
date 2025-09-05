package com.example.good.profile

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.good.R
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.firestore.SetOptions

class EditButtonFragment : Fragment() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText

    private lateinit var confirmPasswordEditText: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var changePhotoButton: Button
    private lateinit var saveButton: Button

    private var imageUri: Uri? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private lateinit var sharedPreferences: SharedPreferences

    private var galleryLauncher: ActivityResultLauncher<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_profile_edit_button, container, false)

        // Firebase 인스턴스
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        // SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("userInfo", Context.MODE_PRIVATE)

        // 툴바 설정
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "프로필 편집"
        }

        // 뷰 초기화
        profileImageView = view.findViewById(R.id.profileImageView)
        usernameEditText = view.findViewById(R.id.usernameEditText)
        passwordEditText = view.findViewById(R.id.etSignUpPassword)
        confirmPasswordEditText = view.findViewById(R.id.etSignUpeditPassword)
        changePhotoButton = view.findViewById(R.id.changePhotoButton)
        saveButton = view.findViewById(R.id.saveButton)

        // 저장된 사용자 이름 불러오기
        val savedUsername: String = sharedPreferences.getString("username", "사용자 프로필 이름") ?: "사용자 프로필 이름"
        usernameEditText.setText(savedUsername)

        // Firebase에서 프로필 사진 불러오기
        setProfileImageFromFirebase()

        // 저장 버튼 클릭
        saveButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // 비밀번호 확인 체크
            if (password.isNotEmpty() && password != confirmPassword) {
                Toast.makeText(requireContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveProfileToFirebase(username, password)

            if (imageUri != null) {
                uploadProfileImage(imageUri!!)
            }
        }

        // 프로필 사진 변경 버튼
        changePhotoButton.setOnClickListener {
            openGallery()
        }

        // 갤러리 런처 등록
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { result: Uri? ->
            if (result != null) {
                imageUri = result
                profileImageView.setImageURI(imageUri)
            }
        }
        // 실시간 비밀번호 확인
        confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = passwordEditText.text.toString()
                val confirm = s.toString()
                if (confirm.isEmpty()) {
                    confirmPasswordEditText.error = null
                    confirmPasswordEditText.setTextColor(Color.BLACK)
                } else if (password == confirm) {
                    confirmPasswordEditText.error = null
                    confirmPasswordEditText.setTextColor(Color.GREEN)
                } else {
                    confirmPasswordEditText.error = "비밀번호가 일치하지 않습니다"
                    confirmPasswordEditText.setTextColor(Color.RED)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })


        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fragment에서 메뉴 클릭 이벤트 사용 가능하도록 설정
        setHasOptionsMenu(true)
        // 로그인 계정에 맞는 닉네임 불러오기
        loadUsernameFromFirebase()
    }
    private fun loadUsernameFromFirebase() {
        val userId = mAuth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "사용자 프로필 이름"
                    usernameEditText.setText(username) // UI에 바로 반영
                    sharedPreferences.edit().putString("username", username).apply() // 캐시 저장
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "닉네임 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // 뒤로가기 버튼 클릭 시 이전 화면으로 이동
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setProfileImageFromFirebase() {
        val userId = mAuth.currentUser?.uid ?: return
        val profileImageRef = storageRef.child("profileImage/$userId/profile.jpg")

        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(requireContext())
                .load(uri)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(profileImageView)
        }.addOnFailureListener {
            // 실패 시 기본 이미지
            profileImageView.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun saveProfileToFirebase(username: String, password: String) {
        val userId = mAuth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        // set을 merge 옵션과 함께 사용하면 기존 데이터 보존 + 덮어쓰기 가능
        val updates = hashMapOf("username" to username)

        userRef.set(updates, SetOptions.merge())
            .addOnSuccessListener {
                // UI에 바로 반영
                usernameEditText.setText(username)
                sharedPreferences.edit().putString("username", username).apply()
                Toast.makeText(requireContext(), "사용자 정보가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "사용자 정보 업데이트 실패", Toast.LENGTH_SHORT).show()
            }

        // 비밀번호 변경
        if (password.isNotEmpty()) {
            mAuth.currentUser?.updatePassword(password)
                ?.addOnSuccessListener {
                    Toast.makeText(requireContext(), "비밀번호 변경 성공", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener {
                    Toast.makeText(requireContext(), "비밀번호 변경 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openGallery() {
        galleryLauncher?.launch("image/*")
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val userId = mAuth.currentUser?.uid ?: return
        val profileImageRef = storageRef.child("profileImage/$userId/profile.jpg")

        profileImageRef.putFile(imageUri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    sharedPreferences.edit()
                        .putString("profileImageUrl", downloadUri.toString())
                        .apply()

                    Toast.makeText(requireContext(), "프로필 사진 업로드 성공", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}