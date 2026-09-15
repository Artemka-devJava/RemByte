package ru.fixbyte.crm.next.data

import kotlinx.serialization.Serializable

class ApiException(message: String) : Exception(message)

@Serializable
data class ClientBriefDto(
    val id: Long,
    val name: String = "",
    val phone: String = "",
    val type: String? = null,
    val tags: String? = null,
    val archivedAt: String? = null
) {
    val archived: Boolean get() = archivedAt != null
}

@Serializable
data class ClientDetailDto(
    val id: Long,
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    val address: String? = null,
    val type: String? = null,
    val tags: String? = null,
    val notes: String? = null,
    val preferredChannel: String? = null,
    val archivedAt: String? = null
) {
    val archived: Boolean get() = archivedAt != null
}

@Serializable
data class ClientSummaryDto(
    val ordersCount: Long = 0,
    val revenue: Double = 0.0,
    val debt: Double = 0.0
)

@Serializable
data class OrderLineDto(
    val name: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1
)

@Serializable
data class OrderBriefDto(
    val id: Long,
    val orderNumber: String = "",
    val status: String = "",
    val deviceDescription: String? = null,
    val totalPrice: Double = 0.0,
    val paidAmount: Double = 0.0,
    val createdAt: String? = null,
    val lines: List<OrderLineDto> = emptyList(),
    val photoUrls: List<String> = emptyList()
)

@Serializable
data class ReminderDto(
    val id: Long,
    val text: String = "",
    val dueAt: String? = null,
    val done: Boolean = false,
    val doneAt: String? = null,
    val clientId: Long? = null,
    val orderId: Long? = null,
    val createdAt: String? = null
)

@Serializable
data class StaleOrderDto(
    val orderId: Long,
    val orderNumber: String = "",
    val clientName: String = "",
    val clientId: Long? = null,
    val status: String = "",
    val since: String? = null
)

@Serializable
data class RemindersResponse(
    val reminders: List<ReminderDto> = emptyList(),
    val staleOrders: List<StaleOrderDto> = emptyList(),
    val staleOrderDays: Int = 30
)

@Serializable
data class ClientPhotoDto(
    val id: Long,
    val url: String = "",
    val caption: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ClientRefRequest(val id: Long)

@Serializable
data class CreateClientRequest(
    val name: String,
    val phone: String,
    val type: String,
    val isActive: Boolean = true
)

@Serializable
data class CreateOrderRequest(
    val client: ClientRefRequest,
    val status: String = "NEW",
    val paidAmount: Double = 0.0,
    val notes: String? = null,
    val deviceDescription: String? = null,
    val lines: List<OrderLineDto> = emptyList()
)

@Serializable
data class UpdateOrderRequest(
    val notes: String? = null,
    val deviceDescription: String? = null,
    val lines: List<OrderLineDto> = emptyList()
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class IdResponse(val id: Long)

@Serializable
data class DuplicateClientResponse(val id: Long, val name: String = "")

@Serializable
data class ErrorResponse(val error: String? = null)

sealed interface CreateClientResult {
    data class Created(val id: Long, val orderFailed: Boolean = false) : CreateClientResult
    data class Duplicate(val id: Long, val name: String) : CreateClientResult
}

fun OrderBriefDto.estimateText(): String? {
    if (lines.isEmpty()) return null
    return lines.joinToString("; ") { "${it.name} — ${formatMoney(it.unitPrice * it.quantity)}" }
        .ifBlank { null }
}

fun formatMoney(value: Double): String =
    if (value == value.toLong().toDouble()) "${value.toLong()} ₽" else "$value ₽"

fun statusRu(status: String): String = when (status.uppercase()) {
    "NEW" -> "новый"
    "IN_PROGRESS", "IN-PROGRESS" -> "в работе"
    "WAITING_FOR_PARTS", "WAITING", "ON_HOLD" -> "ожидание деталей"
    "READY" -> "готов"
    "COMPLETED", "DONE" -> "завершён"
    "CANCELLED", "CANCELED" -> "отменён"
    "ISSUED", "CLOSED" -> "выдан"
    else -> status.lowercase().ifBlank { "—" }
}
