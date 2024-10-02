package com.example.rt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(
    private val fileList: List<FileData>,
    private val onDownloadClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit,
    private val role: String
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileData = fileList[position]
        holder.bind(fileData)

        if (role == "Warga") {
            holder.deleteButton.visibility = View.GONE
        } else {
            holder.deleteButton.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        private val downloadButton: Button = itemView.findViewById(R.id.buttonDownload)
        val deleteButton: Button = itemView.findViewById(R.id.buttonDelete)

        fun bind(fileData: FileData) {
            titleTextView.text = fileData.title

            downloadButton.setOnClickListener {
                onDownloadClick(fileData.fileUrl)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(fileData.fileUrl)
            }
        }
    }
}