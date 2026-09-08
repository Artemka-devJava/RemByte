package ru.fixbyte.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.databinding.ActivitySettingsBinding

/**
 * Настройки подключения: адрес сервера CRM + учётные данные.
 * Логин и пароль вводятся здесь один раз — дальше приложение входит
 * автоматически, в т.ч. после того, как серверная сессия протухла.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.serverUrl.setText(Api.prefs.baseUrl)
        b.username.setText(Api.prefs.lastUsername)
        b.password.setText(Api.prefs.password)

        b.saveButton.setOnClickListener { save() }
    }

    private fun save() {
        val url = b.serverUrl.text?.toString()?.trim().orEmpty()
        if (url.isBlank()) {
            Toast.makeText(this, "Укажите адрес сервера", Toast.LENGTH_SHORT).show()
            return
        }
        Api.prefs.baseUrl = url

        val user = b.username.text?.toString()?.trim().orEmpty()
        val pass = b.password.text?.toString().orEmpty()

        // Адрес поменяли, а логин/пароль не трогали — просто сохраняем адрес.
        if (user.isBlank() && pass.isBlank()) {
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (user.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        setBusy(true)
        lifecycleScope.launch {
            try {
                Api.login(user, pass)   // проверяем и запоминаем — вход дальше автоматический
                Toast.makeText(this@SettingsActivity, "Готово — вход выполнен", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SettingsActivity, ClientsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, e.message ?: "Не удалось войти", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        b.progress.visibility = if (busy) View.VISIBLE else View.GONE
        b.saveButton.isEnabled = !busy
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
