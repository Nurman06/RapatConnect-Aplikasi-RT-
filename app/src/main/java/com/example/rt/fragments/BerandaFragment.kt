package com.example.rt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.rt.databinding.FragmentBerandaBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.content.Context
import android.content.Intent
import com.bumptech.glide.request.RequestOptions

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        val view = binding.root

        // Ambil role yang dikirim dari MainActivity
        val role = arguments?.getString("role") ?: "Warga"
        binding.textViewWelcome.text = "Halo, selamat datang di aplikasi RapatConnect, $role!"

        // Pengecekan role untuk menentukan visibility dari menu pengajuan surat
        if (role == "Warga") {
            binding.menuPengajuanSurat.visibility = View.VISIBLE
        } else {
            binding.menuPengajuanSurat.visibility = View.GONE
        }

        // Ambil data pengguna dari Firestore
        val userId = requireContext().getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""
        if (userId.isNotEmpty()) {
            loadUserData(userId)
        }

        binding.menuTemplateSurat.setOnClickListener {
            val intent = Intent(requireContext(), TemplateSuratActivity::class.java)
            intent.putExtra("role", role) // Kirim role ke TemplateSuratActivity
            startActivity(intent)
        }

        binding.menuPengajuanSurat.setOnClickListener {
            val intent = Intent(requireContext(), PengajuanSuratActivity::class.java)
            intent.putExtra("role", role) // Kirim role ke PengajuanSuratActivity
            startActivity(intent)
        }

        binding.menuPengumuman.setOnClickListener {
            val intent = Intent(requireContext(), PengumumanActivity::class.java)
            intent.putExtra("role", role)
            startActivity(intent)
        }

        binding.menuKritikSaran.setOnClickListener {
            val intent = Intent(requireContext(), KritikSaranActivity::class.java)
            intent.putExtra("role", role) // Mengirimkan role user
            startActivity(intent)
        }

        return view
    }

    private fun loadUserData(userId: String) {
        val db = Firebase.firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("nama") ?: "Nama tidak tersedia"
                val username = document.getString("username") ?: "Username tidak tersedia"
                val email = document.getString("email") ?: "Email tidak tersedia"
                val gender = document.getString("jenis_kelamin") ?: "Jenis kelamin tidak tersedia"
                val birthdate =
                    document.getString("tanggal_lahir") ?: "Tanggal lahir tidak tersedia"
                val profilePictureUrl = document.getString("photoUrl") ?: ""

                // Set data pengguna ke tampilan
                binding.textViewName.text = "Nama: $name"
                binding.textViewUsername.text = "Username: $username"
                binding.textViewEmail.text = "Email: $email"
                binding.textViewGender.text = "Jenis Kelamin: $gender"
                binding.textViewBirthdate.text = "Tanggal Lahir: $birthdate"

                // Load foto profil dengan Glide
                if (profilePictureUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(profilePictureUrl)
                        .apply(RequestOptions.circleCropTransform()) // Menggunakan CircleCrop
                        .into(binding.imageViewProfilePicture)
                }
            }
            .addOnFailureListener {
                // Handle jika gagal mengambil data pengguna
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(role: String): BerandaFragment {
            val fragment = BerandaFragment()
            val args = Bundle()
            args.putString("role", role)
            fragment.arguments = args
            return fragment
        }
    }
}