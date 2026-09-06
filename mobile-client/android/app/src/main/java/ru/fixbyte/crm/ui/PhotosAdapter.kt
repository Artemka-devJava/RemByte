package ru.fixbyte.crm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.ClientPhoto
import ru.fixbyte.crm.databinding.ItemPhotoBinding

class PhotosAdapter(
    private val onOpen: (ClientPhoto) -> Unit,
    private val onShare: (ClientPhoto) -> Unit,
    private val onDelete: (ClientPhoto) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.VH>() {

    private val items = mutableListOf<ClientPhoto>()

    fun submit(list: List<ClientPhoto>) {
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
        fun bind(p: ClientPhoto) {
            b.photo.load(Api.absoluteUrl(p.url))
            b.photo.setOnClickListener { onOpen(p) }
            b.sharePhoto.setOnClickListener { onShare(p) }
            b.deletePhoto.setOnClickListener { onDelete(p) }
        }
    }
}
