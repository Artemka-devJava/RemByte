package ru.fixbyte.crm.next.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.fixbyte.crm.next.data.Api
import ru.fixbyte.crm.next.data.CreateClientResult
import ru.fixbyte.crm.next.ui.common.formatPhoneInput
import ru.fixbyte.crm.next.ui.common.normalizeRuPhone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClientSheet(onDismiss: () -> Unit, onCreated: (Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+7 ") }
    var isCompany by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var duplicate by remember { mutableStateOf<CreateClientResult.Duplicate?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val normalized = normalizeRuPhone(phone)
        if (name.isBlank()) { error = "Укажите имя"; return }
        if (normalized == null) { error = "Некорректный номер телефона"; return }
        error = null
        saving = true
        val price = priceText.replace(',', '.').toDoubleOrNull()
        scope.launch {
            runCatching {
                Api.createClient(
                    name = name.trim(),
                    phone = normalized,
                    type = if (isCompany) "COMPANY" else "INDIVIDUAL",
                    problem = problem.trim().ifBlank { null },
                    estimatePrice = price
                )
            }.onSuccess { result ->
                saving = false
                when (result) {
                    is CreateClientResult.Created -> onCreated(result.id)
                    is CreateClientResult.Duplicate -> duplicate = result
                }
            }.onFailure {
                saving = false
                error = it.message ?: "Не удалось создать клиента"
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Новый клиент", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = formatPhoneInput(it) },
                label = { Text("Телефон") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isCompany, onClick = { isCompany = false }, label = { Text("Физлицо") })
                FilterChip(selected = isCompany, onClick = { isCompany = true }, label = { Text("Организация") })
            }

            Text("Первичная заявка (необязательно)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = problem, onValueChange = { problem = it },
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

            Button(onClick = ::submit, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (saving) "Сохранение…" else "Создать")
            }
        }
    }

    duplicate?.let { dup ->
        AlertDialog(
            onDismissRequest = { duplicate = null },
            title = { Text("Клиент уже существует") },
            text = { Text("Клиент с таким телефоном уже есть: ${dup.name}") },
            confirmButton = {
                TextButton(onClick = { onCreated(dup.id) }) { Text("Открыть") }
            },
            dismissButton = {
                TextButton(onClick = { duplicate = null }) { Text("Отмена") }
            }
        )
    }
}
