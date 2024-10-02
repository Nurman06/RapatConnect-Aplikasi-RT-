package com.example.rt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.rt.databinding.ActivityMainBinding
import com.example.rt.fragments.AktivitasFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil role dari intent yang dikirim dari LoginActivity
        val role = intent.getStringExtra("role") ?: "Warga"

        // Setup bottom navigation bar
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_beranda -> {
                    loadFragment(BerandaFragment.newInstance(role))
                    true
                }

                R.id.navigation_aktivitas -> {
                    loadFragment(AktivitasFragment())
                    true
                }

                R.id.navigation_saya -> {
                    loadFragment(SayaFragment())
                    true
                }

                else -> false
            }
        }

        // Load fragment Beranda saat pertama kali activity dibuka
        binding.bottomNavigation.selectedItemId = R.id.navigation_beranda
    }

    // Fungsi untuk mengganti fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}