package ru.fixbyte.crm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.databinding.ItemPhotoBinding

/**
 * Сетка фотографий по списку URL (относительных или абсолютных).
 * Используется для вложений-фото заказа.
 */
class PhotosAdapter(
    private val onOpen: (String) -> Unit,
    private val onShare: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.VH>() {

    private val items = mutableListOf<String>()

    fun submit(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemPhotoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(url: String) {
            b.photo.load(Api.absoluteUrl(url))
            b.photo.setOnClickListener { onOpen(url) }
            b.sharePhoto.setOnClickListener { onShare(url) }
            b.deletePhoto.setOnClickListener { onDelete(url) }
        }
    }
}
