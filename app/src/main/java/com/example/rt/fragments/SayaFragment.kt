package com.example.rt

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.rt.databinding.FragmentSayaBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class SayaFragment : Fragment() {

    private var _binding: FragmentSayaBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSayaBinding.inflate(inflater, container, false)

        // Inisialisasi Firebase
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Ambil data pengguna dari Firestore
        getUserData()

        // Tombol upload foto
        binding.buttonUploadPhoto.setOnClickListener {
            selectImage()
        }

        // Tombol simpan data
        binding.buttonSimpan.setOnClickListener {
            saveUserData()
        }

        binding.jenisKelaminDropdown.setOnClickListener {
            binding.jenisKelaminDropdown.showDropDown()
        }

        setupTanggalLahir()
        setupJenisKelamin()

        return binding.root
    }

    private fun setupTanggalLahir() {
        binding.tanggalLahirTextInputLayout.setEndIconOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun setupJenisKelamin() {
        val genderOptions = listOf("Laki-laki", "Perempuan")
        val genderAdapter = ArrayAdapter(requireContext(), R.layout.list_item, genderOptions)
        (binding.jenisKelaminDropdown as? AutoCompleteTextView)?.setAdapter(genderAdapter)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.editTextTanggalLahir.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            Glide.with(this).load(imageUri).into(binding.imageViewProfile) // Menampilkan gambar yang dipilih
        }
    }

    private fun getUserData() {
        val prefs = requireActivity().getSharedPreferences("appPrefs", MODE_PRIVATE)
        val userId = prefs.getString("userId", null)

        if (userId != null) {
            val docRef = firestore.collection("users").document(userId)
            docRef.get().addOnSuccessListener { document ->
                if (document != null) {
                    binding.editTextNama.setText(document.getString("nama"))
                    binding.editTextUsername.setText(document.getString("username"))
                    binding.editTextEmail.setText(document.getString("email"))
                    binding.editTextTanggalLahir.setText(document.getString("tanggal_lahir"))

                    // Menampilkan foto profil jika ada
                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this).load(photoUrl).into(binding.imageViewProfile)
                    }
                } else {
                    Toast.makeText(context, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Gagal mengambil data pengguna", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Pengguna tidak terautentikasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserData() {
        val prefs = requireActivity().getSharedPreferences("appPrefs", MODE_PRIVATE)
        val userId = prefs.getString("userId", null)

        if (userId != null) {
            // Simpan foto profil jika ada yang diupload
            if (imageUri != null) {
                val storageRef = storage.reference.child("profile_images/${UUID.randomUUID()}.jpg")
                val uploadTask = storageRef.putFile(imageUri!!)

                uploadTask.addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        updateFirestore(userId, uri.toString())
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "Gagal mengupload foto", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Simpan data tanpa update foto
                updateFirestore(userId, null)
            }
        } else {
            Toast.makeText(context, "Pengguna tidak terautentikasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFirestore(userId: String, photoUrl: String?) {
        val userData: MutableMap<String, Any> = hashMapOf(
            "nama" to binding.editTextNama.text.toString(),
            "username" to binding.editTextUsername.text.toString(),
            "email" to binding.editTextEmail.text.toString(),
            "tanggal_lahir" to binding.editTextTanggalLahir.text.toString(),
            "jenis_kelamin" to binding.jenisKelaminDropdown.text.toString()
        )

        // Jika ada URL foto, tambahkan ke data
        if (photoUrl != null) {
            userData["photoUrl"] = photoUrl
        }

        firestore.collection("users").document(userId)
            .update(userData)
            .addOnSuccessListener {
                Toast.makeText(context, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}