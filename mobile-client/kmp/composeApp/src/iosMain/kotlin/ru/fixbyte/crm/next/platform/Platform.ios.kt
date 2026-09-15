package ru.fixbyte.crm.next.platform

// TODO(iOS): написано по аналогии с mobile-client/ios (CameraPicker.swift/
// ActivityView.swift/OrderDetailView.swift), но НЕ проверено компиляцией —
// в этом окружении нет macOS/Xcode (см. mobile-client/ios/README.md — та же
// оговорка действует и для существующего нативного iOS-клиента). Нужен ревью
// и сборка на Mac перед использованием.

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

private fun topViewController(): UIViewController? {
    var root = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (root?.presentedViewController != null) root = root.presentedViewController
    return root
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val out = ByteArray(size)
    if (size > 0) out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return out
}

private fun writeTemp(bytes: ByteArray, filename: String): NSURL {
    val path = NSTemporaryDirectory() + filename
    bytes.toNSData().writeToFile(path, atomically = true)
    return NSURL.fileURLWithPath(path)
}

actual fun openExternalUri(uri: String) {
    val url = NSURL.URLWithString(uri) ?: return
    if (UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
    }
}

actual fun shareJpeg(bytes: ByteArray, filename: String) {
    val url = writeTemp(bytes, filename)
    val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    topViewController()?.presentViewController(controller, animated = true, completion = null)
}

actual fun sharePdf(bytes: ByteArray, filename: String) = shareJpeg(bytes, filename)

actual fun printPdf(bytes: ByteArray, filename: String) {
    val url = writeTemp(bytes, filename)
    val controller = UIPrintInteractionController.sharedPrintController()
    controller?.printingItem = url
    controller?.presentAnimated(true, completionHandler = null)
}

private class ImagePickerDelegate(
    private val onPicked: (PickedImage) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage ?: return
        val data = UIImageJPEGRepresentation(image, 0.85) ?: return
        onPicked(PickedImage(data.toByteArray(), "photo_${NSDate().timeIntervalSince1970.toLong()}.jpg", "image/jpeg"))
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): ImagePickerHandle {
    val delegate = remember { ImagePickerDelegate(onPicked) }

    fun launch(sourceType: UIImagePickerControllerSourceType) {
        if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) return
        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    return ImagePickerHandle(
        launchCamera = { launch(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera) },
        launchGallery = { launch(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary) }
    )
}
