package com.example.rt

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rt.databinding.ActivityPengajuanSuratBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PengajuanSuratActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPengajuanSuratBinding
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPengajuanSuratBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonUploadFile.setOnClickListener {
            openFileChooser()
        }

        binding.buttonKirimPengajuan.setOnClickListener {
            submitPengajuan()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            fileUri = data.data
            fileUri?.let {
                val fileName = getFileName(it)
                binding.textViewFileSelected.text = fileName
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = ""
        if (uri.scheme.equals("content")) {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    result = cursor.getString(nameIndex)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result.isEmpty()) {
            result = uri.path!!.substring(uri.path!!.lastIndexOf('/') + 1)
        }
        return result
    }

    private fun submitPengajuan() {
        val judul = binding.editTextJudulSurat.text.toString().trim()
        val deskripsi = binding.editTextDeskripsiSurat.text.toString().trim()

        if (judul.isEmpty() || deskripsi.isEmpty() || fileUri == null) {
            Toast.makeText(this, "Mohon lengkapi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        // Upload file ke Firebase Storage
        val prefs = getSharedPreferences("appPrefs", MODE_PRIVATE)
        val pengajuId = prefs.getString("userId", "")
        val storageRef = FirebaseStorage.getInstance().reference.child("surat/$pengajuId/$judul")
        fileUri?.let { uri ->
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    savePengajuanToFirestore(judul, deskripsi, downloadUrl.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal mengunggah file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePengajuanToFirestore(judul: String, deskripsi: String, fileUrl: String) {
        val prefs = getSharedPreferences("appPrefs", MODE_PRIVATE)
        val pengajuId = prefs.getString("userId", "")

        val pengajuan = hashMapOf(
            "judul" to judul,
            "deskripsi" to deskripsi,
            "file_url" to fileUrl,
            "status" to "Menunggu Persetujuan",
            "pengaju" to pengajuId
        )

        FirebaseFirestore.getInstance().collection("pengajuan_surat")
            .add(pengajuan)
            .addOnSuccessListener {
                Toast.makeText(this, "Pengajuan surat berhasil dikirim", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman sebelumnya
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengirim pengajuan surat", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 1
    }
}