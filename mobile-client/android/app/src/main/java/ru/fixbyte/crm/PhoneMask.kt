package ru.fixbyte.crm

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Ввод российского номера телефона: поле всегда начинается с "+7 ",
 * дальше — до 10 цифр. Префикс нельзя стереть/испортить — вотчер
 * восстанавливает его при любом изменении.
 */
private const val PHONE_PREFIX = "+7 "

fun installPhoneMask(edit: EditText) {
    if (edit.text.isNullOrEmpty()) {
        edit.setText(PHONE_PREFIX)
        edit.setSelection(PHONE_PREFIX.length)
    }
    edit.addTextChangedListener(object : TextWatcher {
        private var formatting = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (formatting) return
            val raw = s?.toString().orEmpty()

            val local: String = if (raw.startsWith(PHONE_PREFIX)) {
                raw.substring(PHONE_PREFIX.length)
            } else {
                // Префикс стёрт/вставлен целиком другой номер — разбираем цифры заново.
                raw.filter { it.isDigit() }.removePrefix("7").removePrefix("8")
            }

            var digits = local.filter { it.isDigit() }
            if (digits.length > 10) digits = digits.substring(0, 10)

            val formatted = PHONE_PREFIX + digits
            if (formatted != raw) {
                formatting = true
                edit.setText(formatted)
                edit.setSelection(formatted.length)
                formatting = false
            }
        }
    })
}

/**
 * Приводит введённый номер к виду "+7XXXXXXXXXX".
 * Возвращает null, если после нормализации это не ровно 11 цифр, начинающихся с 7.
 */
fun normalizeRuPhone(input: String): String? {
    var digits = input.filter { it.isDigit() }
    if (digits.length == 11 && digits.startsWith("8")) {
        digits = "7" + digits.substring(1)
    }
    if (digits.length != 11 || !digits.startsWith("7")) return null
    return "+$digits"
}
