package ru.starline.bluz.ui.dialog


import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.starline.bluz.R
import java.io.File

class ChiFilePickerDialog : DialogFragment() {

    private lateinit var chiFiles: List<ChiFileInfo>
    private var listener: OnChiFileSelectedListener? = null

    interface OnChiFileSelectedListener {
        fun onChiFileSelected(filePath: String)
    }

    data class ChiFileInfo(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val lastModified: Long
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Сканируем директорию
        chiFiles = scanChiFiles()

        val adapter = ChiFileAdapter(chiFiles) { fileInfo ->
            listener?.onChiFileSelected(fileInfo.filePath)
            dismiss()
        }

        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
            setPadding(16, 16, 16, 16)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Выберите файл калибровки (.chi)")
            .setView(recyclerView)
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    /**
     * Сканирование директории Documents/BluZ на наличие .chi файлов
     */
    private fun scanChiFiles(): List<ChiFileInfo> {
        val bluZDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "BluZ"
        )

        if (!bluZDir.exists() || !bluZDir.isDirectory) {
            return emptyList()
        }

        return bluZDir.listFiles { file ->
            file.isFile && file.extension.equals("chi", ignoreCase = true)
        }?.map { file ->
            ChiFileInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                lastModified = file.lastModified()
            )
        }?.sortedByDescending { it.lastModified } ?: emptyList()
    }

    fun setOnFileSelectedListener(listener: OnChiFileSelectedListener) {
        this.listener = listener
    }

    // Адаптер для RecyclerView
    private inner class ChiFileAdapter(
        private val files: List<ChiFileInfo>,
        private val onItemClick: (ChiFileInfo) -> Unit
    ) : RecyclerView.Adapter<ChiFileAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fileName: TextView = view.findViewById(R.id.textFileName)
            val fileSize: TextView = view.findViewById(R.id.textFileSize)
            val lastModified: TextView = view.findViewById(R.id.textLastModified)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chi_file, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            holder.fileName.text = file.fileName
            holder.fileSize.text = formatFileSize(file.fileSize)
            holder.lastModified.text = formatDateTime(file.lastModified)
            holder.itemView.setOnClickListener { onItemClick(file) }
        }

        override fun getItemCount() = files.size

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            }
        }

        private fun formatDateTime(timestamp: Long): String {
            val date = java.util.Date(timestamp)
            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(date)
        }
    }
}