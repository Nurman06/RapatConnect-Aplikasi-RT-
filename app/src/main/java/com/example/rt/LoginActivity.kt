package com.example.rt

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rt.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                signInWithUsername(username, password)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }

        setupRegisterTextView()
    }

    private fun setupRegisterTextView() {
        val text = "Belum punya akun? Daftar di sini!"
        val spannableString = SpannableString(text)

        // Set clickable span
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                finish()
                val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Garis bawah terlihat
            }
        }, text.indexOf("Daftar di sini!"), text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.registerTextView.text = spannableString
        binding.registerTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun signInWithUsername(username: String, password: String) {
        val db = Firebase.firestore

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                if (result.documents.isEmpty()) {
                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                } else {
                    // Periksa password
                    val userId = result.documents[0].id
                    checkPassword(userId, password)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPassword(userId: String, password: String) {
        val db = Firebase.firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val storedPassword = document.getString("password") ?: ""
                val hashedPassword = hashPassword(password)

                if (storedPassword == hashedPassword) {
                    // Password benar, simpan userId ke SharedPreferences
                    val prefs = getSharedPreferences("appPrefs", MODE_PRIVATE)
                    prefs.edit().putString("userId", userId).apply()

                    val role = document.getString("role") ?: "Warga"
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("role", role)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking password", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}