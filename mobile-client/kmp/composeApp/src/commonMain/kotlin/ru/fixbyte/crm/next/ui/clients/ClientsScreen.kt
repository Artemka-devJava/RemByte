package ru.fixbyte.crm.next.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.fixbyte.crm.next.data.Api
import ru.fixbyte.crm.next.data.ClientBriefDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onOpenClient: (Long) -> Unit,
    onLoggedOut: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var clients by remember { mutableStateOf<List<ClientBriefDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load(q: String) {
        loading = true
        runCatching { if (q.isBlank()) Api.clients() else Api.searchClients(q) }
            .onSuccess { clients = it; error = null }
            .onFailure { error = it.message ?: "Ошибка загрузки" }
        loading = false
    }

    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(350)
        if (query.isBlank() || query.length >= 2) load(query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Клиенты") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Меню") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Настройки") }, onClick = { menuExpanded = false; onOpenSettings() })
                        DropdownMenuItem(text = { Text("Выйти") }, onClick = {
                            menuExpanded = false
                            scope.launch { Api.logout(); onLoggedOut() }
                        })
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
                Button(
                    onClick = { showCreateSheet = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("+ Новый клиент") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Поиск: имя, телефон, email") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = { scope.launch { load(query) } },
                modifier = Modifier.fillMaxSize()
            ) {
                if (error != null && clients.isEmpty()) {
                    Text(
                        error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(clients, key = { it.id }) { c ->
                        ClientRow(c, onClick = { onOpenClient(c.id) })
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateClientSheet(
            onDismiss = { showCreateSheet = false },
            onCreated = { id ->
                showCreateSheet = false
                scope.launch { load(query) }
                onOpenClient(id)
            }
        )
    }
}

@Composable
private fun ClientRow(client: ClientBriefDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(client.name.ifBlank { "Без имени" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                if (client.archived) {
                    Text("Архив", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(client.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!client.tags.isNullOrBlank()) {
                Text(client.tags, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
