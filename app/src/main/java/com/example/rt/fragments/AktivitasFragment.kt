package com.example.rt.fragments

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rt.PengajuanAdapter
import com.example.rt.PengajuanSurat
import com.example.rt.R
import com.example.rt.ViewSuratActivity
import com.example.rt.databinding.FragmentAktivitasBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.google.firebase.auth.FirebaseAuth
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

class AktivitasFragment : Fragment() {

    private var _binding: FragmentAktivitasBinding? = null
    private val binding get() = _binding!!
    private lateinit var pengajuanAdapter: PengajuanAdapter
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAktivitasBinding.inflate(inflater, container, false)

        // Ambil role dari intent yang dikirim dari LoginActivity
        val role = requireActivity().intent.getStringExtra("role") ?: "Warga"

        // Set up RecyclerView
        pengajuanAdapter = PengajuanAdapter(
            onAcceptClicked = { pengajuan ->
                pengajuan.fileUrl?.let { fileUrl ->
                    downloadFile(fileUrl, { localFile ->
                        val fileUri = Uri.fromFile(localFile)
                        addSignatureToWordFile(
                            requireContext(),
                            fileUri,
                            R.drawable.signature, // Ganti dengan drawable tanda tangan Anda
                            onSuccess = {
                                // File sudah ditandatangani, upload ke Firestore
                                uploadFileToFirestore(localFile, pengajuan.id)
                            },
                            onFailure = { exception ->
                                Toast.makeText(
                                    context,
                                    "Gagal menambahkan tanda tangan: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }, { exception ->
                        Toast.makeText(
                            context,
                            "Gagal mengunduh file: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                }
            },
            onRejectClicked = { pengajuan ->
                updateStatusPengajuan(pengajuan.id, "Ditolak")
            },
            onDownloadClicked = { pengajuan ->
                pengajuan.fileUrl?.let { fileUrl ->
                    downloadFile(fileUrl) // Ganti panggilan untuk downloadFile baru
                }
            },
            role = role
        )

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = pengajuanAdapter

        // Fetch pengajuan surat dari Firestore
        fetchPengajuanSurat(role)

        return binding.root
    }

    private fun addSignatureToWordFile(
        context: Context,
        fileUri: Uri,
        signatureDrawableId: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            // Membuka file Word
            val inputStream = FileInputStream(File(fileUri.path!!))
            val document = XWPFDocument(inputStream)

            // Membaca gambar tanda tangan dari drawable
            val signatureBitmap =
                BitmapFactory.decodeResource(context.resources, signatureDrawableId)

            // Konversi Bitmap menjadi byte array
            val stream = ByteArrayOutputStream()
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // Cari paragraf yang mengandung penanda "{{signature}}"
            val paragraphs = document.paragraphs
            for (paragraph in paragraphs) {
                val text = paragraph.text
                if (text.contains("{{signature}}")) {
                    // Hapus teks "{{signature}}" dari paragraf
                    val runList = paragraph.runs
                    for (run in runList) {
                        if (run.text().contains("{{signature}}")) {
                            run.setText(
                                run.text().replace("{{signature}}", ""),
                                0
                            ) // Menghapus teks penanda
                        }
                    }

                    // Sisipkan gambar tanda tangan di lokasi penanda
                    val run: XWPFRun = paragraph.createRun()

                    val imageStream = ByteArrayInputStream(byteArray)
                    run.addPicture(
                        imageStream,
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "signature.png",
                        Units.toEMU(50.0),  // Lebar gambar dalam EMU
                        Units.toEMU(50.0)   // Tinggi gambar dalam EMU
                    )

                    // Tambahkan baris baru setelah gambar jika diperlukan
                    run.addBreak()

                    break  // Keluar dari loop setelah gambar disisipkan
                }
            }

            // Menyimpan file Word yang sudah dimodifikasi
            val outputFile = File(fileUri.path!!.replace(".docx", "_signed.docx"))
            FileOutputStream(outputFile).use { outputStream ->
                document.write(outputStream)
            }

            // Menutup dokumen
            document.close()

            // Memanggil callback onSuccess
            onSuccess()
        } catch (e: Exception) {
            onFailure(e) // Panggil callback onFailure jika ada error
        }
    }

    private fun fetchPengajuanSurat(role: String) {
        when (role) {
            "Warga" -> {
                val prefs = requireContext().getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
                val pengajuId = prefs.getString("userId", "")

                db.collection("pengajuan_surat")
                    .whereEqualTo("pengaju", pengajuId)
                    .get()
                    .addOnSuccessListener { result ->
                        val pengajuanList = result.map { doc ->
                            PengajuanSurat(
                                doc.id,
                                doc.getString("judul") ?: "",
                                doc.getString("deskripsi") ?: "",
                                doc.getString("file_url") ?: "",
                                doc.getString("status") ?: ""
                            )
                        }
                        pengajuanAdapter.submitList(pengajuanList)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal memuat pengajuan surat", Toast.LENGTH_SHORT).show()
                    }
            }
            "Ketua RT" -> {
                db.collection("pengajuan_surat")
                    .whereEqualTo("status", "Menunggu Persetujuan")
                    .get()
                    .addOnSuccessListener { result ->
                        val pengajuanList = result.map { doc ->
                            PengajuanSurat(
                                doc.id,
                                doc.getString("judul") ?: "",
                                doc.getString("deskripsi") ?: "",
                                doc.getString("file_url") ?: "",
                                doc.getString("status") ?: ""
                            )
                        }
                        pengajuanAdapter.submitList(pengajuanList)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal memuat pengajuan surat", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun downloadFile(
        fileUrl: String,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileName = fileUrl.substringAfterLast("/")
        val localFile = File(requireContext().cacheDir, fileName)

        // Mengunduh file dari URL
        Thread {
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()

                val input: InputStream = connection.inputStream
                val output = FileOutputStream(localFile)

                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }

                output.close()
                input.close()

                onSuccess(localFile)
            } catch (e: Exception) {
                onFailure(e)
            }
        }.start()
    }

    private fun uploadFileToFirestore(localFile: File, pengajuanId: String) {
        val storageRef = storage.reference
        val fileUri = Uri.fromFile(localFile)
        val fileRef = storageRef.child("signed_files/$pengajuanId.docx") // Ubah sesuai kebutuhan

        // Upload file ke Firebase Storage
        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                // Mendapatkan URL file yang diupload
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    // Update URL file di Firestore dan ubah status pengajuan menjadi 'Disetujui'
                    updateFileUrlInFirestore(pengajuanId, uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Gagal mengupload file yang ditandatangani: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateFileUrlInFirestore(pengajuanId: String, fileUrl: String) {
        // Update status pengajuan dan URL file di Firestore
        db.collection("pengajuan_surat").document(pengajuanId)
            .update("file_url", fileUrl, "status", "Disetujui")
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    "File yang ditandatangani berhasil diupload dan status pengajuan diperbarui",
                    Toast.LENGTH_SHORT
                ).show()
                pengajuanAdapter.removeItem(pengajuanId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Gagal memperbarui URL file: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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

        val downloadManager =
            requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(requireContext(), "File is downloading...", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusPengajuan(pengajuanId: String, status: String) {
        db.collection("pengajuan_surat").document(pengajuanId)
            .update("status", status)
            .addOnSuccessListener {
                Toast.makeText(context, "Status pengajuan berhasil diperbarui", Toast.LENGTH_SHORT)
                    .show()
                pengajuanAdapter.removeItem(pengajuanId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Gagal memperbarui status pengajuan: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}