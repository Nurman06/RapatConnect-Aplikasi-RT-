package com.example.rt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rt.databinding.ItemPengajuanBinding

class PengajuanAdapter(
    private val onAcceptClicked: (PengajuanSurat) -> Unit,
    private val onRejectClicked: (PengajuanSurat) -> Unit,
    private val onDownloadClicked: (PengajuanSurat) -> Unit,
    private val role: String
) : RecyclerView.Adapter<PengajuanAdapter.PengajuanViewHolder>() {

    private var pengajuanList: List<PengajuanSurat> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PengajuanViewHolder {
        val binding =
            ItemPengajuanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PengajuanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PengajuanViewHolder, position: Int) {
        val pengajuan = pengajuanList[position]
        holder.bind(pengajuan, onAcceptClicked, onRejectClicked, onDownloadClicked, role)
    }

    override fun getItemCount(): Int = pengajuanList.size

    fun submitList(list: List<PengajuanSurat>) {
        pengajuanList = list
        notifyDataSetChanged()
    }

    fun removeItem(pengajuanId: String) {
        val position = pengajuanList.indexOfFirst { it.id == pengajuanId }
        if (position != -1) {
            pengajuanList = pengajuanList.filter { it.id != pengajuanId }
            notifyItemRemoved(position)
        }
    }

    class PengajuanViewHolder(private val binding: ItemPengajuanBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            pengajuan: PengajuanSurat,
            onAcceptClicked: (PengajuanSurat) -> Unit,
            onRejectClicked: (PengajuanSurat) -> Unit,
            onDownloadClicked: (PengajuanSurat) -> Unit,
            role: String
        ) {
            binding.textViewJudul.text = pengajuan.judul
            binding.textViewDeskripsi.text = pengajuan.deskripsi
            binding.textViewStatus.text = pengajuan.status

            when (role) {
                "Warga" -> {
                    if (pengajuan.status == "Disetujui") {
                        binding.buttonDownload.visibility = View.VISIBLE
                        binding.buttonAccept.visibility = View.GONE
                        binding.buttonReject.visibility = View.GONE
                    } else {
                        binding.buttonDownload.visibility = View.GONE
                        binding.buttonAccept.visibility = View.GONE
                        binding.buttonReject.visibility = View.GONE
                    }
                }
                "Ketua RT" -> {
                    binding.buttonDownload.visibility = View.GONE
                    if (pengajuan.status == "Menunggu Persetujuan") {
                        binding.buttonAccept.visibility = View.VISIBLE
                        binding.buttonReject.visibility = View.VISIBLE
                    } else {
                        binding.buttonAccept.visibility = View.GONE
                        binding.buttonReject.visibility = View.GONE
                    }
                }
            }

            binding.buttonAccept.setOnClickListener {
                onAcceptClicked(pengajuan)
            }
            binding.buttonReject.setOnClickListener {
                onRejectClicked(pengajuan)
            }
            binding.buttonDownload.setOnClickListener {
                onDownloadClicked(pengajuan)
            }
        }
    }
}