package com.example.rt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rt.databinding.ItemBalasanBinding

class BalasanAdapter(private val listBalasan: List<Balasan>) :
    RecyclerView.Adapter<BalasanAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBalasanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBalasanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val balasan = listBalasan[position]
        holder.binding.tvBalasanUserName.text = holder.itemView.context.getString(R.string.user_name_with_role, balasan.userName, balasan.userRole)
        holder.binding.tvBalasanKonten.text = balasan.konten

        Glide.with(holder.itemView.context).load(balasan.userPhotoUrl)
            .placeholder(R.drawable.ic_person)
            .into(holder.binding.imgUserProfileBalasan)
    }

    override fun getItemCount() = listBalasan.size
}