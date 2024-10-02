package com.example.rt

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rt.databinding.ActivityReplyKritikSaranBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth

class ReplyKritikSaranActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReplyKritikSaranBinding
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private lateinit var kritikSaranId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReplyKritikSaranBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Balas Kritik dan Saran"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Ambil ID kritik/saran dari Intent
        kritikSaranId = intent.getStringExtra("kritikSaranId") ?: ""

        // Inisialisasi RecyclerView untuk daftar balasan
        binding.recyclerViewBalasan.layoutManager = LinearLayoutManager(this)
        loadBalasan()

        // Kirim balasan
        binding.btnKirimBalasan.setOnClickListener {
            val balasan = binding.editTextBalasan.text.toString().trim()
            if (balasan.isNotEmpty()) {
                submitBalasan(balasan)
            } else {
                Toast.makeText(this, "Isi balasan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadBalasan() {
        db.collection("kritikSaran")
            .document(kritikSaranId)
            .collection("balasan")
            .get()
            .addOnSuccessListener { result ->
                val listBalasan = mutableListOf<Balasan>()
                result.forEach { document ->
                    val userId = document.getString("userId") ?: ""
                    val userRef = db.collection("users").document(userId)

                    userRef.get().addOnSuccessListener { userDocument ->
                        val userName = userDocument.getString("nama") ?: ""
                        val userRole = userDocument.getString("role") ?: ""
                        val userPhotoUrl = userDocument.getString("photoUrl") ?: ""

                        listBalasan.add(
                            Balasan(
                                konten = document.getString("konten") ?: "",
                                userName = userName,
                                userRole = userRole,
                                userPhotoUrl = userPhotoUrl
                            )
                        )

                        if (listBalasan.size == result.size()) {
                            binding.recyclerViewBalasan.adapter = BalasanAdapter(listBalasan)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal memuat balasan: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun submitBalasan(balasan: String) {
        val prefs = getSharedPreferences("appPrefs", MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""

        // Ambil informasi pengguna dari Firestore berdasarkan userId
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Data yang akan disimpan di Firestore
                    val data = hashMapOf(
                        "konten" to balasan,
                        "userId" to userId
                    )

                    // Simpan balasan ke koleksi "balasan" di dalam dokumen kritik/saran
                    db.collection("kritikSaran")
                        .document(kritikSaranId)
                        .collection("balasan")
                        .add(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Balasan berhasil dikirim", Toast.LENGTH_SHORT).show()
                            loadBalasan() // Muat ulang daftar balasan
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal mengirim balasan", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data pengguna: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}