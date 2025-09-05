package com.example.good.Login

import android.os.Bundle
import android.graphics.Color
import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.good.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var editTextName: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextBirthDate: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextDetailAddress: EditText
    private lateinit var radioButtonMale: RadioButton
    private lateinit var radioButtonFemale: RadioButton
    private lateinit var buttonRegister: Button

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editTextEmail = findViewById(R.id.etEmail)
        editTextPassword = findViewById(R.id.etSignUpPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        editTextName = findViewById(R.id.etName)
        editTextUsername = findViewById(R.id.etUsername)
        editTextPhone = findViewById(R.id.etPhoneNumber)
        editTextBirthDate = findViewById(R.id.etBirthDate)
        editTextAddress = findViewById(R.id.etAddress)
        editTextDetailAddress = findViewById(R.id.etDetailAddress)
        radioButtonMale = findViewById(R.id.rbMale)
        radioButtonFemale = findViewById(R.id.rbFemale)
        buttonRegister = findViewById(R.id.btnSubmitSignUp)

        // 날짜 선택 다이얼로그
        editTextBirthDate.setOnClickListener { showDatePickerDialog() }

        // 회원가입 버튼 클릭
        buttonRegister.setOnClickListener { registerUser() }

        // 비밀번호 확인 실시간 체크
        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = editTextPassword.text.toString()
                val confirm = s.toString()
                if (confirm.isEmpty()) {
                    etConfirmPassword.error = null
                    etConfirmPassword.setTextColor(Color.BLACK)
                } else if (password == confirm) {
                    etConfirmPassword.error = null
                    etConfirmPassword.setTextColor(Color.GREEN)
                } else {
                    etConfirmPassword.error = "비밀번호가 일치하지 않습니다"
                    etConfirmPassword.setTextColor(Color.RED)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })


    }


    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year1, monthOfYear, dayOfMonth ->
            val date = "$year1-${monthOfYear + 1}-$dayOfMonth"
            editTextBirthDate.setText(date)
        }, year, month, day).show()
    }


    private fun registerUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val name = editTextName.text.toString().trim()
        val username = editTextUsername.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val birthDate = editTextBirthDate.text.toString().trim()
        val address = editTextAddress.text.toString().trim()
        val detailAddress = editTextDetailAddress.text.toString().trim()
        val gender = if (radioButtonMale.isChecked) "Male" else "Female"
        val signUpDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || username.isEmpty()
            || phone.isEmpty() || birthDate.isEmpty() || address.isEmpty()
            || detailAddress.isEmpty() || gender.isEmpty()
        ) {
            Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_LONG).show()
            return
        }
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = User(
                        name, username, email, phone, gender,
                        birthDate, address, detailAddress, signUpDate
                    )

                    db.collection("users").document(mAuth.currentUser!!.uid).set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "회원가입 성공", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "회원 정보 등록 실패", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    data class User(
        val name: String,
        val username: String,
        val email: String,
        val phone: String,
        val gender: String,
        val birthDate: String,
        val address: String,
        val detailAddress: String,
        val signUpDate: String
    )
}
