package ru.fixbyte.crm.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.ClientDetail
import ru.fixbyte.crm.OrderBrief
import ru.fixbyte.crm.databinding.ActivityClientBinding

class ClientActivity : AppCompatActivity() {

    private lateinit var b: ActivityClientBinding
    private lateinit var ordersAdapter: OrdersAdapter
    private var clientId: Long = 0
    private var detail: ClientDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityClientBinding.inflate(layoutInflater)
        setContentView(b.root)

        clientId = intent.getLongExtra(ClientsActivity.EXTRA_CLIENT_ID, 0)
        if (clientId == 0L) { finish(); return }

        b.toolbar.setNavigationOnClickListener { finish() }

        ordersAdapter = OrdersAdapter(onClick = ::openOrder)
        b.ordersList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        b.ordersList.adapter = ordersAdapter
        b.addOrderButton.setOnClickListener { newOrderDialog() }

        b.callButton.setOnClickListener { dial() }
        b.writeButton.setOnClickListener { write() }

        loadAll()
    }

    private fun loadAll() {
        setBusy(true)
        lifecycleScope.launch {
            try {
                val d = Api.client(clientId)
                detail = d
                b.toolbar.title = d.name
                b.name.text = if (d.archived) "${d.name}  · архив" else d.name
                b.subline.text = listOfNotNull(
                    d.phone.ifBlank { null },
                    if (d.type == "COMPANY") "Организация" else "Физлицо"
                ).joinToString("  ·  ")

                val s = runCatching { Api.summary(clientId) }.getOrNull()
                b.summary.text = if (s == null) "" else
                    "Заказов: ${s.ordersCount}     Оборот: ${fmt(s.revenue)}     Долг: ${fmt(s.debt)}"
                b.summary.visibility = if (s == null) View.GONE else View.VISIBLE

                b.details.text = buildString {
                    d.email?.let { append("Email: ").append(it).append('\n') }
                    d.address?.let { append("Адрес: ").append(it).append('\n') }
                    d.tags?.let { append("Метки: ").append(it).append('\n') }
                    d.notes?.let { append("Заметка: ").append(it) }
                }.trim()
                b.details.visibility = if (b.details.text.isBlank()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@ClientActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                if (e.message?.contains("Сессия истекла") == true) toLogin()
            } finally {
                setBusy(false)
            }
            loadOrders()
        }
    }

    override fun onResume() {
        super.onResume()
        // вернулись с экрана заказа — список мог измениться
        if (clientId != 0L && ::ordersAdapter.isInitialized) loadOrders()
    }

    // ── Заказы ────────────────────────────────────────────────

    private fun loadOrders() {
        lifecycleScope.launch {
            val list = runCatching { Api.clientOrders(clientId) }.getOrDefault(emptyList())
            ordersAdapter.submit(list)
            b.ordersEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openOrder(o: OrderBrief) {
        startActivity(Intent(this, OrderActivity::class.java).apply {
            putExtra(OrderActivity.EXTRA_ORDER_ID, o.id)
            putExtra(OrderActivity.EXTRA_CLIENT_ID, clientId)
        })
    }

    private fun newOrderDialog() {
        val pad = (resources.displayMetrics.density * 20).toInt()
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        val problem = android.widget.EditText(this).apply {
            hint = "Что принято / неисправность"
            setLines(2)
        }
        val price = android.widget.EditText(this).apply {
            hint = "Примерная стоимость, ₽"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        box.addView(problem)
        box.addView(price)

        AlertDialog.Builder(this)
            .setTitle("Новый заказ")
            .setView(box)
            .setPositiveButton("Создать") { _, _ ->
                val p = problem.text?.toString()?.trim()
                val amount = price.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
                if (p.isNullOrEmpty() && amount == null) {
                    Toast.makeText(this, "Заполните хотя бы одно поле", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val id = Api.createOrder(clientId, p, amount)
                        loadOrders()
                        if (id > 0) startActivity(Intent(this@ClientActivity, OrderActivity::class.java).apply {
                            putExtra(OrderActivity.EXTRA_ORDER_ID, id)
                            putExtra(OrderActivity.EXTRA_CLIENT_ID, clientId)
                        })
                    } catch (e: Exception) {
                        Toast.makeText(this@ClientActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ── Связь ─────────────────────────────────────────────────

    private fun dial() {
        val d = detail ?: return
        val digits = d.phone.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) return
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    }

    private fun write() {
        val d = detail ?: return
        val digits = d.phone.filter { it.isDigit() }
        val email = d.email
        val intent: Intent? = when {
            d.preferredChannel == "TELEGRAM" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+$digits"))
            d.preferredChannel == "WHATSAPP" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
            !email.isNullOrBlank() -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
            digits.isNotBlank() -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
            else -> null
        }
        if (intent != null) runCatching { startActivity(intent) }
    }

    // ── Прочее ────────────────────────────────────────────────

    private fun setBusy(busy: Boolean) {
        b.progress.visibility = if (busy) View.VISIBLE else View.GONE
    }

    private fun fmt(v: Double) = "${v.toLong()}₽"

    private fun toLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
