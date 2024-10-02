package com.example.rt

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rt.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Menambahkan pilihan untuk dropdown jenis kelamin dan role
        setupDropdownAdapters()

        // Tambahkan listener untuk dropdown jenis kelamin
        binding.jenisKelaminDropdown.setOnClickListener {
            binding.jenisKelaminDropdown.showDropDown()
        }

        // Tambahkan listener untuk dropdown role
        binding.roleDropdown.setOnClickListener {
            binding.roleDropdown.showDropDown()
        }

        // Panggil DatePickerDialog ketika EditText untuk tanggal lahir diklik
        binding.tanggalLahirTextInputLayout.setEndIconOnClickListener {
            showDatePickerDialog()
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val nama = binding.namaEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val jenisKelamin = binding.jenisKelaminDropdown.text.toString()
            val role = binding.roleDropdown.text.toString()
            val tanggalLahir = binding.tanggalLahirEditText.text.toString().trim()

            if (username.isNotEmpty() && nama.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() &&
                jenisKelamin.isNotEmpty() && tanggalLahir.isNotEmpty()
            ) {

                if (!email.endsWith("@gmail.com")) {
                    Toast.makeText(this, "Email must be @gmail.com", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password.length < 6) {
                    Toast.makeText(
                        this,
                        "Password must be at least 6 characters long",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                checkIfEmailExists(email) { emailExists ->
                    if (emailExists) {
                        Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show()
                        return@checkIfEmailExists
                    }

                    checkIfUsernameExists(username) { usernameExists ->
                        if (usernameExists) {
                            Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT)
                                .show()
                            return@checkIfUsernameExists
                        }

                        val hashedPassword = hashPassword(password)
                        auth.createUserWithEmailAndPassword(email, hashedPassword)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    saveUserToFirestore(
                                        username,
                                        nama,
                                        email,
                                        jenisKelamin,
                                        tanggalLahir,
                                        role
                                    )
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Register failed: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
        setupLoginTextView()
    }

    private fun setupLoginTextView() {
        val text = "Sudah punya akun? Masuk di sini!"
        val spannableString = SpannableString(text)

        // Set clickable span
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Menutup RegisterActivity
                finish()
                // Membuka LoginActivity
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Mengatur agar garis bawah terlihat
            }
        }, text.indexOf("Masuk di sini!"), text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set text ke TextView dan buat dapat diklik
        binding.loginTextView.text = spannableString
        binding.loginTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    // Fungsi untuk menginisialisasi adapter dropdown
    private fun setupDropdownAdapters() {
        val genderOptions = listOf("Laki-laki", "Perempuan")
        val genderAdapter = ArrayAdapter(this, R.layout.list_item, genderOptions)
        (binding.jenisKelaminDropdown as? AutoCompleteTextView)?.setAdapter(genderAdapter)

        val roleOptions = listOf("Warga", "Ketua RT")
        val roleAdapter = ArrayAdapter(this, R.layout.list_item, roleOptions)
        (binding.roleDropdown as? AutoCompleteTextView)?.setAdapter(roleAdapter)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.tanggalLahirEditText.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun checkIfEmailExists(email: String, callback: (Boolean) -> Unit) {
        val db = Firebase.firestore
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                callback(result.documents.isNotEmpty())
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun checkIfUsernameExists(username: String, callback: (Boolean) -> Unit) {
        val db = Firebase.firestore
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                callback(result.documents.isNotEmpty())
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun saveUserToFirestore(
        username: String,
        nama: String,
        email: String,
        jenisKelamin: String,
        tanggalLahir: String,
        role: String
    ) {
        val db = Firebase.firestore
        val userData = hashMapOf(
            "username" to username,
            "nama" to nama,
            "email" to email,
            "jenis_kelamin" to jenisKelamin,
            "tanggal_lahir" to tanggalLahir,
            "role" to role,
            "password" to hashPassword(binding.passwordEditText.text.toString().trim())
        )
        auth.currentUser?.let { currentUser ->
            db.collection("users").document(currentUser.uid)
                .set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent) // Start LoginActivity
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                }
        }
    }
}