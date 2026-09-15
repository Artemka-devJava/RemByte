package ru.fixbyte.crm.next.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.fixbyte.crm.next.AppContext

private const val CHANNEL_ID = "reminders"

private fun ensureChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = AppContext.context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Напоминания", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}

// Реальный runtime-запрос разрешения POST_NOTIFICATIONS делает MainActivity
// (нужен Activity, а не просто Context) — здесь только гарантируем канал.
actual fun ensureNotificationPermission() {
    ensureChannel()
}

actual fun showNotification(id: String, title: String, body: String) {
    val context = AppContext.context
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    ensureChannel()
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    runCatching { NotificationManagerCompat.from(context).notify(id.hashCode(), notification) }
}
