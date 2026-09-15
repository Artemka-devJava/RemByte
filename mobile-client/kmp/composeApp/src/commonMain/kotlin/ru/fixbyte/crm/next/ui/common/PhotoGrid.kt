package ru.fixbyte.crm.next.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import ru.fixbyte.crm.next.platform.ImagePickerHandle

data class GridPhoto(val id: Long, val url: String, val caption: String? = null)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoGrid(
    photos: List<GridPhoto>,
    picker: ImagePickerHandle,
    onDelete: (GridPhoto) -> Unit,
    onShare: (GridPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullscreen by remember { mutableStateOf<GridPhoto?>(null) }
    var pendingDelete by remember { mutableStateOf<GridPhoto?>(null) }

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = picker.launchCamera) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Камера")
            }
            OutlinedButton(onClick = picker.launchGallery) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Галерея")
            }
        }

        if (photos.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photos.forEach { photo ->
                    AsyncImage(
                        model = photo.url,
                        contentDescription = photo.caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { fullscreen = photo }
                    )
                }
            }
        }
    }

    fullscreen?.let { photo ->
        Dialog(onDismissRequest = { fullscreen = null }) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = photo.caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    Modifier.align(Alignment.TopEnd).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { onShare(photo) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Поделиться", tint = Color.White)
                    }
                    IconButton(onClick = { pendingDelete = photo }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = Color.White)
                    }
                    IconButton(onClick = { fullscreen = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                }
            }
        }
    }

    pendingDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить фото?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(photo)
                    pendingDelete = null
                    fullscreen = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } }
        )
    }
}
