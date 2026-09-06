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

        b.serverUrl.setText(Api.prefs.baseUrl)
        b.username.setText(Api.prefs.lastUsername)

        b.loginButton.setOnClickListener { doLogin() }

        // Уже авторизованы? — сразу к клиентам.
        lifecycleScope.launch {
            if (Api.prefs.baseUrl.isNotBlank() && Api.isLoggedIn()) openClients()
        }
    }

    private fun doLogin() {
        val url = b.serverUrl.text?.toString()?.trim().orEmpty()
        val user = b.username.text?.toString()?.trim().orEmpty()
        val pass = b.password.text?.toString().orEmpty()

        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        Api.prefs.baseUrl = url
        setBusy(true)
        lifecycleScope.launch {
            try {
                Api.login(user, pass)
                Api.prefs.lastUsername = user
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
