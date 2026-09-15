package ru.fixbyte.crm.next.ui.common

private const val PHONE_PREFIX = "+7 "

/** Переформатировать ввод номера: поле всегда "+7 " + до 10 цифр. */
fun formatPhoneInput(raw: String): String {
    val local = if (raw.startsWith(PHONE_PREFIX)) {
        raw.substring(PHONE_PREFIX.length)
    } else {
        raw.filter { it.isDigit() }.removePrefix("7").removePrefix("8")
    }
    val digits = local.filter { it.isDigit() }.take(10)
    return PHONE_PREFIX + digits
}

/** "+7 XXX XXX XX XX" -> "+7XXXXXXXXXX", либо null если не 11 цифр начиная с 7/8. */
fun normalizeRuPhone(input: String): String? {
    var digits = input.filter { it.isDigit() }
    if (digits.length == 11 && digits.startsWith("8")) digits = "7" + digits.substring(1)
    if (digits.length != 11 || !digits.startsWith("7")) return null
    return "+$digits"
}
