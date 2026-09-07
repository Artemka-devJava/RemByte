package ru.fixbyte.crm.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.databinding.ActivitySettingsBinding

/**
 * Настройки подключения: адрес сервера CRM.
 * Отдельно от экрана входа — задаётся один раз при установке приложения.
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

        b.saveButton.setOnClickListener {
            val url = b.serverUrl.text?.toString()?.trim().orEmpty()
            if (url.isBlank()) {
                Toast.makeText(this, "Укажите адрес сервера", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Api.prefs.baseUrl = url
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
