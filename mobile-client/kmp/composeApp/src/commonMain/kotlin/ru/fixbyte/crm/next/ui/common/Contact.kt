package ru.fixbyte.crm.next.ui.common

import ru.fixbyte.crm.next.data.ClientDetailDto

fun callUri(client: ClientDetailDto): String? {
    val phone = client.phone.filter { it.isDigit() || it == '+' }
    return phone.ifBlank { null }?.let { "tel:$it" }
}

/** (uri, подпись кнопки) — повторяет логику client-card.js#updateContactButtons. */
fun writeContact(client: ClientDetailDto): Pair<String, String> {
    val digits = client.phone.filter { it.isDigit() }
    return when {
        client.preferredChannel == "TELEGRAM" -> "https://t.me/+$digits" to "Telegram"
        client.preferredChannel == "WHATSAPP" -> "https://wa.me/$digits" to "WhatsApp"
        !client.email.isNullOrBlank() -> "mailto:${client.email}" to "Email"
        else -> "https://wa.me/$digits" to "Написать"
    }
}
