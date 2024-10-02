package com.example.rt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rt.databinding.ItemPengumumanBinding

class PengumumanAdapter(
    private val pengumumanList: List<Pengumuman>,
    private val onDeleteClick: (String) -> Unit, // Parameter untuk listener
    private val userRole: String // Add this parameter
) : RecyclerView.Adapter<PengumumanAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPengumumanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemPengumumanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pengumuman = pengumumanList[position]
        holder.binding.textViewHeadline.text = pengumuman.judul
        holder.binding.textViewSupportingText.text = pengumuman.deskripsi
        holder.binding.textViewSubhead.text = pengumuman.tanggal

        // Menampilkan gambar
        Glide.with(holder.itemView.context)
            .load(pengumuman.fileUrl)
            .into(holder.binding.imageViewPengumuman)

        // Handle tombol expand
        holder.binding.iconExpand.setOnClickListener {
            if (holder.binding.textViewSupportingText.maxLines == 3) {
                holder.binding.textViewSupportingText.maxLines = Int.MAX_VALUE // Expand
                holder.binding.iconExpand.setImageResource(R.drawable.ic_expand_less) // Ganti ikon
            } else {
                holder.binding.textViewSupportingText.maxLines = 3 // Collapse
                holder.binding.iconExpand.setImageResource(R.drawable.ic_expand_more) // Ganti ikon
            }
        }

        // Handle tombol delete
        if (userRole == "Warga") {
            holder.binding.buttonDelete.visibility = View.GONE // Hide delete button for Warga role
        } else {
            holder.binding.buttonDelete.setOnClickListener {
                onDeleteClick(pengumuman.fileUrl) // Kirim URL untuk dihapus
            }
        }
    }

    override fun getItemCount() = pengumumanList.size
}