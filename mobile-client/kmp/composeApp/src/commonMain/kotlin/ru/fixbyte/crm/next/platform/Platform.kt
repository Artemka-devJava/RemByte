package ru.fixbyte.crm.next.platform

import androidx.compose.runtime.Composable

data class PickedImage(val bytes: ByteArray, val filename: String, val mime: String)

class ImagePickerHandle(val launchCamera: () -> Unit, val launchGallery: () -> Unit)

/** Снимок с камеры или выбор из галереи; отдаёт уже уменьшенный (~1600px) JPEG. */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage) -> Unit): ImagePickerHandle

/** Открыть внешний URI: tel:, https://wa.me/…, https://t.me/…, mailto:. */
expect fun openExternalUri(uri: String)

/** Поделиться фото через системный шэринг (мессенджеры/соцсети/почта). */
expect fun shareJpeg(bytes: ByteArray, filename: String)

/** Поделиться PDF-файлом через системный шэринг. */
expect fun sharePdf(bytes: ByteArray, filename: String)

/** Напечатать PDF через системную печать (Mopria/AirPrint). */
expect fun printPdf(bytes: ByteArray, filename: String)
