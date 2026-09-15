package ru.fixbyte.crm.next.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ru.fixbyte.crm.next.AppContext
import java.io.ByteArrayOutputStream
import java.io.File

private fun authority() = "${AppContext.context.packageName}.fileprovider"

private fun downscaleJpeg(bytes: ByteArray, maxDimension: Int = 1600): ByteArray {
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
    val scale = maxDimension.toFloat() / maxOf(original.width, original.height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
    } else {
        original
    }
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    if (bitmap !== original) bitmap.recycle()
    original.recycle()
    return out.toByteArray()
}

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): ImagePickerHandle {
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null) {
            val bytes = downscaleJpeg(file.readBytes())
            onPicked(PickedImage(bytes, "photo_${System.currentTimeMillis()}.jpg", "image/jpeg"))
        }
        file?.delete()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = AppContext.context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) onPicked(PickedImage(downscaleJpeg(bytes), "photo_${System.currentTimeMillis()}.jpg", "image/jpeg"))
        }
    }

    fun startCamera() {
        val dir = File(AppContext.context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
        pendingCameraFile = file
        val uri = FileProvider.getUriForFile(AppContext.context, authority(), file)
        runCatching { cameraLauncher.launch(uri) }
    }

    // Раз манифест объявляет android.permission.CAMERA, системный intent
    // ACTION_IMAGE_CAPTURE падает с SecurityException, пока разрешение не
    // выдано явно — даже несмотря на то, что снимает не наш код, а
    // сторонее приложение "Камера".
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera()
    }

    return ImagePickerHandle(
        launchCamera = {
            val granted = ContextCompat.checkSelfPermission(AppContext.context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) startCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        launchGallery = { galleryLauncher.launch("image/*") }
    )
}

actual fun openExternalUri(uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { AppContext.context.startActivity(intent) }
}

actual fun shareJpeg(bytes: ByteArray, filename: String) {
    val dir = File(AppContext.context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, filename)
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(AppContext.context, authority(), file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AppContext.context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

actual fun sharePdf(bytes: ByteArray, filename: String) {
    val dir = File(AppContext.context.cacheDir, "acts").apply { mkdirs() }
    val file = File(dir, filename)
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(AppContext.context, authority(), file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AppContext.context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

actual fun printPdf(bytes: ByteArray, filename: String) {
    val dir = File(AppContext.context.cacheDir, "acts").apply { mkdirs() }
    val file = File(dir, filename)
    file.writeBytes(bytes)
    val printManager = AppContext.context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
    printManager.print(filename, PdfPrintDocumentAdapter(file, filename), null)
}
