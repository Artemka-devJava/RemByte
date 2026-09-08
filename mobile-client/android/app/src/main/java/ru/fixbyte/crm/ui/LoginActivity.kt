package ru.fixbyte.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.ApiException
import ru.fixbyte.crm.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.username.setText(Api.prefs.lastUsername)
        b.loginButton.setOnClickListener { doLogin() }
        b.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        autoLoginOrShowForm()
    }

    override fun onResume() {
        super.onResume()
        // Вернулись из «Настроек», где могли вписать логин/пароль — пробуем войти.
        if (b.loginForm.visibility == View.VISIBLE && Api.prefs.hasCredentials()) {
            autoLoginOrShowForm()
        }
    }

    /**
     * Форма входа скрыта, пока идёт проверка — чтобы поля не «мелькали», если
     * вход уже возможен. Если есть сохранённые логин/пароль, приложение входит
     * само; форма показывается только когда автоматически войти не удалось.
     */
    private fun autoLoginOrShowForm() {
        b.loginForm.visibility = View.GONE
        b.startupProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            if (Api.ensureLoggedIn()) {
                openClients()
            } else {
                b.username.setText(Api.prefs.lastUsername)
                b.startupProgress.visibility = View.GONE
                b.loginForm.visibility = View.VISIBLE
            }
        }
    }

    private fun doLogin() {
        val user = b.username.text?.toString()?.trim().orEmpty()
        val pass = b.password.text?.toString().orEmpty()

        if (user.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        setBusy(true)
        lifecycleScope.launch {
            try {
                Api.login(user, pass)   // логин/пароль сохраняются внутри — вход дальше автоматический
                openClients()
            } catch (e: ApiException) {
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Сеть недоступна: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        b.loginButton.isEnabled = !busy
        b.loginProgress.visibility = if (busy) View.VISIBLE else View.GONE
    }

    private fun openClients() {
        startActivity(Intent(this, ClientsActivity::class.java))
        finish()
    }
}
