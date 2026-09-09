package ru.fixbyte.crm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.fixbyte.crm.OrderBrief
import ru.fixbyte.crm.databinding.ItemOrderBinding

class OrdersAdapter(
    private val onClick: (OrderBrief) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.VH>() {

    private val items = mutableListOf<OrderBrief>()

    fun submit(list: List<OrderBrief>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemOrderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(o: OrderBrief) {
            b.orderNumber.text = o.number.ifBlank { "Заказ №${o.id}" }
            b.orderStatus.text = statusRu(o.status)

            val device = o.deviceDescription?.trim().orEmpty()
            b.orderDevice.visibility = if (device.isEmpty()) View.GONE else View.VISIBLE
            b.orderDevice.text = device

            val sum = if (o.total > 0) money(o.total) else "оценка не задана"
            val date = o.createdAt?.take(10)?.split("-")?.reversed()?.joinToString(".") ?: ""
            b.orderMeta.text = listOf(sum, date).filter { it.isNotBlank() }.joinToString("  ·  ")

            b.root.setOnClickListener { onClick(o) }
        }
    }

    companion object {
        fun statusRu(s: String): String = when (s.uppercase()) {
            "NEW" -> "новый"
            "IN_PROGRESS", "IN-PROGRESS" -> "в работе"
            "WAITING", "ON_HOLD" -> "ожидание"
            "COMPLETED", "DONE" -> "готов"
            "CANCELLED", "CANCELED" -> "отменён"
            "ISSUED", "CLOSED" -> "выдан"
            else -> s.lowercase().ifBlank { "—" }
        }

        fun money(v: Double): String =
            if (v == v.toLong().toDouble()) "${v.toLong()} ₽" else "$v ₽"
    }
}
