package com.example.good.Login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.good.main.MainActivity
import com.example.good.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbAutoLogin: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var mAuth: FirebaseAuth

    private val PREFS_NAME = "user_prefs"
    private val KEY_AUTO_LOGIN = "auto_login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etUserid)
        etPassword = findViewById(R.id.etPassword)
        cbAutoLogin = findViewById(R.id.cbLoginauto)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUp)

        // 자동로그인 체크
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoLogin = prefs.getBoolean(KEY_AUTO_LOGIN, false)
        if (autoLogin && mAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnLogin.setOnClickListener { loginUser() }
        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                // 자동 로그인 저장
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_AUTO_LOGIN, cbAutoLogin.isChecked).apply()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val exception = task.exception
                when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> Toast.makeText(this, "잘못된 비밀번호입니다.", Toast.LENGTH_SHORT).show()
                    is FirebaseAuthInvalidUserException -> Toast.makeText(this, "존재하지 않는 이메일입니다.", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this, "로그인 실패: ${exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}