package ru.fixbyte.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.CreateClientResult
import ru.fixbyte.crm.databinding.ActivityClientsBinding
import ru.fixbyte.crm.databinding.DialogCreateClientBinding

class ClientsActivity : AppCompatActivity() {

    private lateinit var b: ActivityClientsBinding
    private lateinit var adapter: ClientsAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityClientsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        adapter = ClientsAdapter { c -> openClient(c.id) }
        b.clientsList.layoutManager = LinearLayoutManager(this)
        b.clientsList.adapter = adapter

        b.swipe.setOnRefreshListener { load(b.searchField.text?.toString().orEmpty()) }

        b.searchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                load(b.searchField.text?.toString().orEmpty()); true
            } else false
        }
        b.searchField.doAfterTextChanged { editable ->
            val text = editable?.toString().orEmpty()
            searchJob?.cancel()
            searchJob = lifecycleScope.launch { delay(350); load(text) }
        }

        b.addClientFab.setOnClickListener { showCreateClientDialog() }
    }

    override fun onResume() {
        super.onResume()
        load(b.searchField.text?.toString().orEmpty())
    }

    private fun openClient(id: Long) {
        startActivity(Intent(this, ClientActivity::class.java).putExtra(EXTRA_CLIENT_ID, id))
    }

    private fun load(query: String) {
        b.swipe.isRefreshing = true
        lifecycleScope.launch {
            try {
                val list = if (query.trim().length >= 2) Api.searchClients(query.trim())
                else Api.clients().sortedBy { it.name.lowercase() }
                adapter.submit(list)
                b.emptyLabel.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@ClientsActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                if (e.message?.contains("Сессия истекла") == true) goToLogin()
            } finally {
                b.swipe.isRefreshing = false
            }
        }
    }

    // ── Создание клиента ──────────────────────────────────────

    private fun showCreateClientDialog() {
        val d = DialogCreateClientBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Новый клиент")
            .setView(d.root)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Создать", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = d.dcName.text?.toString()?.trim().orEmpty()
                val phone = d.dcPhone.text?.toString()?.trim().orEmpty()
                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Имя и телефон обязательны", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val type = if (d.dcCompany.isChecked) "COMPANY" else "INDIVIDUAL"
                lifecycleScope.launch {
                    try {
                        when (val res = Api.createClient(name, phone, type)) {
                            is CreateClientResult.Created -> {
                                dialog.dismiss()
                                Toast.makeText(this@ClientsActivity, "Клиент создан", Toast.LENGTH_SHORT).show()
                                openClient(res.id)
                            }
                            is CreateClientResult.Duplicate -> {
                                dialog.dismiss()
                                MaterialAlertDialogBuilder(this@ClientsActivity)
                                    .setMessage("Клиент с этим телефоном уже есть: ${res.name}. Открыть карточку?")
                                    .setPositiveButton("Открыть") { _, _ -> openClient(res.id) }
                                    .setNegativeButton("Отмена", null)
                                    .show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ClientsActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    // ── Меню ──────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_LOGOUT, 0, "Выйти").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_LOGOUT) {
            lifecycleScope.launch { Api.logout(); goToLogin() }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        const val EXTRA_CLIENT_ID = "client_id"
        private const val MENU_LOGOUT = 1
    }
}
