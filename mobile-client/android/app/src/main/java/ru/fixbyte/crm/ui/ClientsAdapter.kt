package ru.fixbyte.crm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.fixbyte.crm.ClientBrief
import ru.fixbyte.crm.databinding.ItemClientBinding

class ClientsAdapter(
    private val onClick: (ClientBrief) -> Unit
) : RecyclerView.Adapter<ClientsAdapter.VH>() {

    private val items = mutableListOf<ClientBrief>()

    fun submit(list: List<ClientBrief>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemClientBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: ClientBrief) {
            b.clientName.text = if (c.archived) "${c.name}  · архив" else c.name
            b.clientPhone.text = c.phone
            val tags = c.tags?.trim().orEmpty()
            if (tags.isEmpty()) {
                b.clientTags.visibility = android.view.View.GONE
            } else {
                b.clientTags.visibility = android.view.View.VISIBLE
                b.clientTags.text = tags
            }
            b.root.setOnClickListener { onClick(c) }
        }
    }
}
