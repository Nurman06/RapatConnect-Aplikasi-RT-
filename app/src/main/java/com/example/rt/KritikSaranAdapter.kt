package com.example.rt

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rt.databinding.ItemKritikSaranBinding

class KritikSaranAdapter(private val listKritikSaran: List<KritikSaran>) :
    RecyclerView.Adapter<KritikSaranAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemKritikSaranBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKritikSaranBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kritikSaran = listKritikSaran[position]

        // Set user name and kritik/saran content
        holder.binding.tvUserName.text = holder.itemView.context.getString(R.string.user_name_with_role, kritikSaran.userName, kritikSaran.userRole)
        holder.binding.tvKritikSaran.text = kritikSaran.konten

        // Load user profile image with Glide
        Glide.with(holder.itemView.context)
            .load(kritikSaran.userPhotoUrl)
            .placeholder(R.drawable.ic_person)  // Placeholder jika foto tidak ada
            .into(holder.binding.imgUserProfile)

        // Handle button click (Balas button)
        holder.binding.btnBalas.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ReplyKritikSaranActivity::class.java)
            intent.putExtra("kritikSaranId", kritikSaran.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = listKritikSaran.size
}