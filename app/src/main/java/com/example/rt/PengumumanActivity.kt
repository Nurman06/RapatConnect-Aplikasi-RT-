package com.example.rt

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rt.databinding.ActivityPengumumanBinding
import com.example.rt.databinding.DialogUploadPengumumanBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class PengumumanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPengumumanBinding
    private lateinit var dialogBinding: DialogUploadPengumumanBinding
    private var selectedFileUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPengumumanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Pengumuman RT"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerViewPengumuman.layoutManager = LinearLayoutManager(this)
        fetchPengumuman()

    }



    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create the DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dialogBinding.editTextTanggal.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePickerDialog.show() // Show the DatePickerDialog
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pengumuman, menu)

        val uploadMenuItem = menu?.findItem(R.id.action_upload)
        val actionView = uploadMenuItem?.actionView
        actionView?.setPadding(16, 0, 16, 0)

        actionView?.setOnClickListener {
            showUploadDialog()
        }

        val role = intent.getStringExtra("role") ?: "Warga"
        if (role == "Warga") {
            menu?.findItem(R.id.action_upload)?.isVisible = false
        }


        return true
    }

    private fun showUploadDialog() {
        dialogBinding = DialogUploadPengumumanBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Upload Pengumuman")
            .setView(dialogBinding.root)
            .setPositiveButton("Finish", null)
            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        dialogBinding.buttonSelectImage.setOnClickListener {
            chooseFile()
        }

        dialogBinding.tanggalTextInputLayout.setEndIconOnClickListener {
            showDatePickerDialog()
        }

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val title = dialogBinding.editTextJudul.text.toString()
                val description = dialogBinding.editTextDeskripsi.text.toString()
                val date = dialogBinding.editTextTanggal.text.toString()

                if (title.isNotEmpty() && description.isNotEmpty() && date.isNotEmpty()) {
                    selectedFileUri?.let { uri ->
                        uploadFileToFirebase(title, description, date, uri)
                        dialog.dismiss()
                    } ?: Toast.makeText(this, "Silakan pilih gambar", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun chooseFile() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                dialogBinding.imageViewSelectedImage.setImageURI(uri)
                selectedFileUri = uri
            }
        }
    }

    private fun uploadFileToFirebase(
        title: String,
        description: String,
        date: String,
        fileUri: Uri
    ) {
        val storageRef = storage.reference.child("pengumuman/${UUID.randomUUID()}.jpg")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    savePengumumanToFirestore(title, description, date, downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengupload gambar: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun savePengumumanToFirestore(
        title: String,
        description: String,
        date: String,
        fileUrl: String
    ) {
        val pengumuman = Pengumuman(title, description, date, fileUrl)

        db.collection("pengumuman")
            .add(pengumuman)
            .addOnSuccessListener {
                Toast.makeText(this, "Pengumuman berhasil disimpan", Toast.LENGTH_SHORT).show()
                fetchPengumuman() // Refresh data setelah berhasil
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan pengumuman: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun fetchPengumuman() {
        db.collection("pengumuman")
            .get()
            .addOnSuccessListener { result ->
                val pengumumanList = mutableListOf<Pengumuman>()
                for (document in result) {
                    val pengumuman = document.toObject(Pengumuman::class.java)
                    pengumumanList.add(pengumuman)
                }
                binding.recyclerViewPengumuman.adapter =
                    PengumumanAdapter(
                        pengumumanList,
                        { fileUrl -> deletePengumuman(fileUrl) },
                        intent.getStringExtra("role") ?: "Warga" // Pass user role as an argument
                    )
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat pengumuman: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun deletePengumuman(fileUrl: String) {
        // Menghapus pengumuman dari Firestore
        db.collection("pengumuman")
            .whereEqualTo("fileUrl", fileUrl) // Mencari pengumuman berdasarkan fileUrl
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Menghapus file dari Firebase Storage
                    val storageRef = storage.getReferenceFromUrl(fileUrl)
                    storageRef.delete()
                        .addOnSuccessListener {
                            // Setelah berhasil menghapus file, hapus dokumen dari Firestore
                            db.collection("pengumuman").document(document.id).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Pengumuman berhasil dihapus",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    fetchPengumuman() // Refresh data setelah berhasil dihapus
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Gagal menghapus pengumuman: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Gagal menghapus gambar: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menemukan pengumuman: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}