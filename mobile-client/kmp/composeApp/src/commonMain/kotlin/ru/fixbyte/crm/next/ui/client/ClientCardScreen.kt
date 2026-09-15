package ru.fixbyte.crm.next.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.fixbyte.crm.next.data.Api
import ru.fixbyte.crm.next.data.ClientDetailDto
import ru.fixbyte.crm.next.data.ClientSummaryDto
import ru.fixbyte.crm.next.data.OrderBriefDto
import ru.fixbyte.crm.next.data.formatMoney
import ru.fixbyte.crm.next.data.statusRu
import ru.fixbyte.crm.next.platform.openExternalUri
import ru.fixbyte.crm.next.platform.rememberImagePicker
import ru.fixbyte.crm.next.platform.shareJpeg
import ru.fixbyte.crm.next.ui.common.GridPhoto
import ru.fixbyte.crm.next.ui.common.PhotoGrid
import ru.fixbyte.crm.next.ui.common.callUri
import ru.fixbyte.crm.next.ui.common.writeContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientCardScreen(clientId: Long, onBack: () -> Unit, onOpenOrder: (Long) -> Unit) {
    var client by remember { mutableStateOf<ClientDetailDto?>(null) }
    var summary by remember { mutableStateOf<ClientSummaryDto?>(null) }
    var photos by remember { mutableStateOf<List<GridPhoto>>(emptyList()) }
    var orders by remember { mutableStateOf<List<OrderBriefDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewOrder by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reloadPhotos() {
        runCatching { Api.photos(clientId) }.onSuccess { list ->
            photos = list.map { GridPhoto(it.id, Api.absoluteUrl(it.url), it.caption) }
        }
    }

    suspend fun reload() {
        runCatching {
            client = Api.client(clientId)
            summary = Api.summary(clientId)
            orders = Api.clientOrders(clientId)
        }.onFailure { error = it.message }
        reloadPhotos()
    }

    LaunchedEffect(clientId) { reload() }

    val picker = rememberImagePicker { picked ->
        scope.launch {
            runCatching { Api.uploadPhoto(clientId, picked.bytes, picked.filename, picked.mime, null) }
                .onSuccess { reloadPhotos() }
                .onFailure { error = it.message }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.name ?: "Клиент") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                }
            )
        }
    ) { padding ->
        val current = client
        if (current == null) {
            if (error != null) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(padding).padding(16.dp))
            } else {
                Column(Modifier.padding(padding).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(current.phone, style = MaterialTheme.typography.bodyLarge)
                if (!current.tags.isNullOrBlank()) Text(current.tags, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                if (!current.notes.isNullOrBlank()) Text(current.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                callUri(current)?.let { uri ->
                    OutlinedButton(onClick = { openExternalUri(uri) }) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Позвонить")
                    }
                }
                val (writeUri, label) = writeContact(current)
                OutlinedButton(onClick = { openExternalUri(writeUri) }) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" $label")
                }
            }

            summary?.let { s ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryStat("Заказов", s.ordersCount.toString())
                        SummaryStat("Оборот", formatMoney(s.revenue))
                        SummaryStat("Долг", formatMoney(s.debt), highlight = s.debt > 0)
                    }
                }
            }

            HorizontalDivider()

            Text("Фото", style = MaterialTheme.typography.titleMedium)
            PhotoGrid(
                photos = photos,
                picker = picker,
                onDelete = { photo ->
                    scope.launch {
                        runCatching { Api.deletePhoto(clientId, photo.id) }.onSuccess { reloadPhotos() }
                    }
                },
                onShare = { photo ->
                    scope.launch {
                        runCatching { Api.downloadBytes(photo.url) }
                            .onSuccess { shareJpeg(it, "photo_${photo.id}.jpg") }
                    }
                }
            )

            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Заказы", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showNewOrder = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Новый заказ")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                orders.forEach { order ->
                    Card(onClick = { onOpenOrder(order.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.orderNumber.ifBlank { "№?" }, fontWeight = FontWeight.SemiBold)
                                Text(statusRu(order.status), color = MaterialTheme.colorScheme.primary)
                            }
                            if (!order.deviceDescription.isNullOrBlank()) {
                                Text(order.deviceDescription, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(formatMoney(order.totalPrice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (orders.isEmpty()) Text("Заказов пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showNewOrder) {
        NewOrderSheet(
            onDismiss = { showNewOrder = false },
            onCreate = { description, price ->
                showNewOrder = false
                scope.launch {
                    runCatching { Api.createOrder(clientId, description, price) }
                        .onSuccess { orderId ->
                            reload()
                            onOpenOrder(orderId)
                        }
                        .onFailure { error = it.message }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewOrderSheet(onDismiss: () -> Unit, onCreate: (String?, Double?) -> Unit) {
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Новый заказ", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Проблема с ПК / устройством") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = priceText, onValueChange = { priceText = it },
                label = { Text("Примерная цена, ₽") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    if (description.isBlank()) {
                        error = "Укажите проблему с устройством"
                        return@Button
                    }
                    onCreate(description.trim(), priceText.replace(',', '.').toDoubleOrNull())
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Создать") }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
