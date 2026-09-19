package ru.fixbyte.crm.next.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.fixbyte.crm.next.data.Api
import ru.fixbyte.crm.next.data.OrderBriefDto
import ru.fixbyte.crm.next.data.estimateText
import ru.fixbyte.crm.next.data.formatMoney
import ru.fixbyte.crm.next.data.statusRu
import ru.fixbyte.crm.next.platform.printPdf
import ru.fixbyte.crm.next.platform.rememberImagePicker
import ru.fixbyte.crm.next.platform.shareJpeg
import ru.fixbyte.crm.next.platform.sharePdf
import ru.fixbyte.crm.next.ui.common.GridPhoto
import ru.fixbyte.crm.next.ui.common.PhotoGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(orderId: Long, onBack: () -> Unit) {
    var order by remember { mutableStateOf<OrderBriefDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var actBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        runCatching { Api.order(orderId) }
            .onSuccess { order = it; error = null }
            .onFailure { error = it.message }
    }

    LaunchedEffect(orderId) { reload() }

    val picker = rememberImagePicker { picked ->
        scope.launch {
            runCatching { Api.uploadOrderPhoto(orderId, picked.bytes, picked.filename) }
                .onSuccess { reload() }
                .onFailure { error = it.message }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(order?.orderNumber?.ifBlank { "Заказ" } ?: "Заказ") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                }
            )
        }
    ) { padding ->
        val current = order
        if (current == null) {
            Column(Modifier.padding(padding).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) else CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ошибка после того, как заказ уже открылся (например, не
            // загрузилось фото) — раньше error только выводился, пока current
            // == null, и дальше молча пропадал из виду при неудачной загрузке фото.
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Статус", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(statusRu(current.status), color = MaterialTheme.colorScheme.primary)
                    }
                    if (!current.deviceDescription.isNullOrBlank()) {
                        HorizontalDivider()
                        Text("Проблема", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(current.deviceDescription)
                    }
                    current.estimateText()?.let {
                        HorizontalDivider()
                        Text("Оценка", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(it)
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Итого: ${formatMoney(current.totalPrice)}")
                        Text("Оплачено: ${formatMoney(current.paidAmount)}")
                    }
                }
            }

            OutlinedButton(onClick = { showEdit = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Изменить")
            }

            HorizontalDivider()
            Text("Фото", style = MaterialTheme.typography.titleMedium)
            PhotoGrid(
                photos = current.photoUrls.mapIndexed { i, url -> GridPhoto(i.toLong(), Api.absoluteUrl(url)) },
                picker = picker,
                onDelete = { photo ->
                    scope.launch {
                        runCatching { Api.deleteOrderAttachment(orderId, photo.url) }.onSuccess { reload() }
                    }
                },
                onShare = { photo ->
                    scope.launch {
                        runCatching { Api.downloadBytes(photo.url) }
                            .onSuccess { shareJpeg(it, "order_$orderId.jpg") }
                    }
                }
            )

            HorizontalDivider()
            Text("Акт приёмки", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !actBusy,
                    onClick = {
                        actBusy = true
                        scope.launch {
                            runCatching { Api.acceptanceActPdf(orderId) }
                                .onSuccess { printPdf(it, "act_${current.orderNumber}.pdf") }
                                .onFailure { error = it.message }
                            actBusy = false
                        }
                    }
                ) { Text("Печать") }
                OutlinedButton(
                    enabled = !actBusy,
                    onClick = {
                        actBusy = true
                        scope.launch {
                            runCatching { Api.acceptanceActPdf(orderId) }
                                .onSuccess { sharePdf(it, "act_${current.orderNumber}.pdf") }
                                .onFailure { error = it.message }
                            actBusy = false
                        }
                    }
                ) { Text("Отправить") }
            }
        }
    }

    if (showEdit) {
        EditOrderDialog(
            initialDescription = order?.deviceDescription.orEmpty(),
            onDismiss = { showEdit = false },
            onSave = { description, price ->
                showEdit = false
                scope.launch {
                    runCatching { Api.updateOrder(orderId, description, price) }
                        .onSuccess { reload() }
                        .onFailure { error = it.message }
                }
            }
        )
    }
}

@Composable
private fun EditOrderDialog(
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, Double?) -> Unit
) {
    var description by remember { mutableStateOf(initialDescription) }
    var priceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить заказ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(description, priceText.replace(',', '.').toDoubleOrNull()) }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
