package com.example.rt

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rt.databinding.ActivityTemplateSuratBinding
import com.example.rt.databinding.DialogUploadFileBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class TemplateSuratActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplateSuratBinding
    private lateinit var dialogBinding: DialogUploadFileBinding
    private var selectedFileUri: Uri? = null
    private lateinit var adapter: FileAdapter
    private val fileList = ArrayList<FileData>()
    private lateinit var recyclerView: RecyclerView

    private val filePickerResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                selectedFileUri = uri
                val fileName = getFileName(uri)
                dialogBinding.editTextFileName.setText(fileName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplateSuratBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = binding.recyclerViewFiles
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        supportActionBar?.title = "Template Surat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        val role = intent.getStringExtra("role") ?: "Warga"

        adapter = FileAdapter(
            fileList,
            onDownloadClick = { fileUrl -> downloadFile(fileUrl) },
            onDeleteClick = { fileUrl -> deleteDocumentFile(fileUrl) },
            role
        )
        recyclerView.adapter = adapter

        fetchDataFromFirestore() // Mengambil data dari Firestore
    }

    private fun downloadFile(fileUrl: String) {
        val request = DownloadManager.Request(Uri.parse(fileUrl))
            .setTitle("Downloading File")
            .setDescription("File is being downloaded...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                Uri.parse(fileUrl).lastPathSegment
            )

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(this, "File is downloading...", Toast.LENGTH_SHORT).show()
    }

    private fun fetchDataFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("files").get()
            .addOnSuccessListener { documents ->
                if (fileList.isEmpty()) {
                    fileList.addAll(documents.map { document ->
                        document.toObject(FileData::class.java)
                    })
                    adapter.notifyItemRangeInserted(0, fileList.size) // Notify the adapter that new items have been inserted
                } else {
                    val oldSize = fileList.size
                    fileList.clear()
                    fileList.addAll(documents.map { document ->
                        document.toObject(FileData::class.java)
                    })
                    adapter.notifyItemRangeRemoved(0, oldSize) // Notify the adapter that old items have been removed
                    adapter.notifyItemRangeInserted(0, fileList.size) // Notify the adapter that new items have been inserted
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to retrieve data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteDocumentFile(fileUrl: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Mencari dokumen di Firestore berdasarkan URL file
        firestore.collection("files").whereEqualTo("fileUrl", fileUrl).get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // Hapus dokumen dari Firestore
                    firestore.collection("files").document(document.id).delete()
                        .addOnSuccessListener {
                            // Menghapus file dari Firebase Storage
                            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
                            storageReference.delete()
                                .addOnSuccessListener {
                                    // Menghapus item dari daftar dan memperbarui RecyclerView
                                    val position = fileList.indexOfFirst { it.fileUrl == fileUrl }
                                    if (position != -1) {
                                        fileList.removeAt(position) // Hapus item berdasarkan posisi
                                        adapter.notifyItemRemoved(position) // Hanya beri tahu item yang dihapus
                                        Toast.makeText(this, "File berhasil dihapus", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Gagal menghapus file dari storage", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal menghapus file dari database", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menemukan file di database", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_template_surat, menu)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUploadDialog() {
        dialogBinding = DialogUploadFileBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Upload File")
            .setView(dialogBinding.root)
            .setPositiveButton("Finish") { dialogInterface, _ ->
                val title = dialogBinding.editTextTitle.text.toString()
                selectedFileUri?.let { uri ->
                    uploadFileToFirebase(title, uri)
                } ?: Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show()
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        dialogBinding.FileNameTextInputLayout.setEndIconOnClickListener {
            chooseFile()
        }

        dialog.show()
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/msword"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        intent.putExtra(
            Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )

        filePickerResultLauncher.launch(Intent.createChooser(intent, "Pilih File"))
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            result = cursor.getString(nameIndex)
            cursor.close()
        }
        return result ?: uri.lastPathSegment?.substringAfterLast("/") ?: "Unknown File"
    }

    private fun uploadFileToFirebase(title: String, fileUri: Uri) {
        val storageReference = FirebaseStorage.getInstance().reference
        val fileRef = storageReference.child("uploads/${System.currentTimeMillis()}_${getFileName(fileUri)}")

        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveDataToFirestore(title, downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveDataToFirestore(title: String, fileUrl: String) {
        val firestore = FirebaseFirestore.getInstance()
        val fileData = hashMapOf(
            "title" to title,
            "fileUrl" to fileUrl
        )

        firestore.collection("files").add(fileData)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    fetchDataFromFirestore()
                } else {
                    Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                }
            }
    }
}