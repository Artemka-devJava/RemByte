package ru.fixbyte.crm.next

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ru.fixbyte.crm.next.data.Api
import ru.fixbyte.crm.next.platform.ensureNotificationPermission
import ru.fixbyte.crm.next.reminders.ReminderChecker
import ru.fixbyte.crm.next.theme.FixByteTheme
import ru.fixbyte.crm.next.ui.client.ClientCardScreen
import ru.fixbyte.crm.next.ui.clients.ClientsScreen
import ru.fixbyte.crm.next.ui.common.InstallAuthenticatedImageLoader
import ru.fixbyte.crm.next.ui.login.LoginScreen
import ru.fixbyte.crm.next.ui.order.OrderScreen
import ru.fixbyte.crm.next.ui.settings.SettingsScreen

private sealed interface Route {
    data object Login : Route
    data object Clients : Route
    data class ClientCard(val id: Long) : Route
    data class Order(val id: Long) : Route
    data object Settings : Route
}

@Composable
fun App() {
    FixByteTheme {
        InstallAuthenticatedImageLoader()

        val backStack = remember { mutableStateListOf<Route>(Route.Login) }
        var checkingSession by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        fun push(route: Route) = backStack.add(route)
        fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
        fun toLogin() { backStack.clear(); backStack.add(Route.Login) }

        // Раз вошли — просим разрешение на уведомления и сразу проверяем,
        // нет ли уже наступивших напоминаний/просроченных заказов (дальше на
        // Android эту же проверку периодически повторяет фоновый воркер).
        fun onSignedIn() {
            backStack.clear()
            backStack.add(Route.Clients)
            scope.launch {
                ensureNotificationPermission()
                ReminderChecker.checkAndNotify()
            }
        }

        LaunchedEffect(Unit) {
            if (Api.ensureLoggedIn()) onSignedIn()
            checkingSession = false
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (checkingSession) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                when (val route = backStack.last()) {
                    Route.Login -> LoginScreen(onLoggedIn = { onSignedIn() })

                    Route.Clients -> ClientsScreen(
                        onOpenClient = { push(Route.ClientCard(it)) },
                        onLoggedOut = { toLogin() },
                        onOpenSettings = { push(Route.Settings) }
                    )

                    is Route.ClientCard -> ClientCardScreen(
                        clientId = route.id,
                        onBack = { pop() },
                        onOpenOrder = { orderId -> push(Route.Order(orderId)) }
                    )

                    is Route.Order -> OrderScreen(orderId = route.id, onBack = { pop() })

                    Route.Settings -> SettingsScreen(onBack = { pop() }, onLoggedOut = { toLogin() })
                }
            }
        }
    }
}
