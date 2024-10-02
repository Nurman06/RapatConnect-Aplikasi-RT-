package com.example.rt

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rt.databinding.ActivityKritikSaranBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth

class KritikSaranActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKritikSaranBinding
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance() // Inisialisasi FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKritikSaranBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Kritik dan Saran"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val role = intent.getStringExtra("role") ?: "Warga"

        // Inisialisasi RecyclerView untuk daftar kritik dan saran
        binding.recyclerViewKritikSaran.layoutManager = LinearLayoutManager(this)
        loadKritikSaran()

        // Jika role warga, tampilkan form untuk mengirimkan kritik/saran
        if (role == "Warga") {
            binding.formKritikSaran.visibility = View.VISIBLE
            binding.btnKirim.setOnClickListener {
                val kritikSaran = binding.editTextKritikSaran.text.toString().trim()
                if (kritikSaran.isNotEmpty()) {
                    submitKritikSaran(kritikSaran)
                } else {
                    Toast.makeText(this, "Isi kritik/saran tidak boleh kosong", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            binding.formKritikSaran.visibility = View.GONE
        }
    }

    private fun loadKritikSaran() {
        val listKritikSaran = mutableListOf<KritikSaran>()

        db.collection("kritikSaran")
            .get()
            .addOnSuccessListener { result ->
                result.forEach { document ->
                    val userId = document.getString("userId") ?: ""
                    val userRef = db.collection("users").document(userId)

                    userRef.get().addOnSuccessListener { userDocument ->
                        val userName = userDocument.getString("nama") ?: ""
                        val userRole = userDocument.getString("role") ?: ""
                        val userPhotoUrl = userDocument.getString("photoUrl") ?: ""

                        listKritikSaran.add(
                            KritikSaran(
                                id = document.id,
                                konten = document.getString("konten") ?: "",
                                userName = userName,
                                userPhotoUrl = userPhotoUrl,
                                userRole = userRole
                            )
                        )

                        if (listKritikSaran.size == result.size()) {
                            binding.recyclerViewKritikSaran.adapter = KritikSaranAdapter(listKritikSaran)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Gagal memuat kritik dan saran: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun submitKritikSaran(kritikSaran: String) {
        val prefs = getSharedPreferences("appPrefs", MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""

        // Ambil informasi pengguna dari Firestore berdasarkan userId
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Data yang akan disimpan di Firestore
                    val data = hashMapOf(
                        "konten" to kritikSaran,
                        "userId" to userId
                    )

                    // Simpan kritik/saran ke koleksi "kritikSaran"
                    db.collection("kritikSaran")
                        .add(data)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Kritik dan saran berhasil dikirim",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadKritikSaran() // Muat ulang daftar kritik/saran
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Gagal mengirim kritik dan saran",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(this, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Gagal mengambil data pengguna: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}