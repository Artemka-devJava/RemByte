package ru.fixbyte.crm.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import kotlinx.coroutines.launch
import ru.fixbyte.crm.Api
import ru.fixbyte.crm.OrderBrief
import ru.fixbyte.crm.databinding.ActivityOrderBinding
import java.io.ByteArrayOutputStream
import java.io.File

class OrderActivity : AppCompatActivity() {

    private lateinit var b: ActivityOrderBinding
    private lateinit var photosAdapter: PhotosAdapter
    private var orderId: Long = 0
    private var clientId: Long = 0
    private var current: OrderBrief? = null
    private var pendingCameraUri: Uri? = null

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            when {
                !ok -> Unit
                uri == null -> Toast.makeText(this, "Не удалось получить фото, попробуйте ещё раз", Toast.LENGTH_LONG).show()
                else -> uploadFromUri(uri)
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uploadFromUri(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(b.root)

        pendingCameraUri = savedInstanceState?.getString(STATE_CAMERA_URI)?.let(Uri::parse)

        orderId = intent.getLongExtra(EXTRA_ORDER_ID, 0)
        clientId = intent.getLongExtra(EXTRA_CLIENT_ID, 0)
        if (orderId == 0L) { finish(); return }

        b.toolbar.setNavigationOnClickListener { finish() }
        b.printActButton.setOnClickListener { printAct() }
        b.shareActButton.setOnClickListener { shareAct() }
        b.editButton.setOnClickListener { editDialog() }

        photosAdapter = PhotosAdapter(onOpen = ::openFullscreen, onShare = ::sharePhoto, onDelete = ::confirmDeletePhoto)
        b.photosGrid.layoutManager = GridLayoutManager(this, 3)
        b.photosGrid.adapter = photosAdapter
        b.cameraButton.setOnClickListener { launchCamera() }
        b.galleryButton.setOnClickListener { pickImage.launch("image/*") }

        load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingCameraUri?.let { outState.putString(STATE_CAMERA_URI, it.toString()) }
    }

    private fun load() {
        setBusy(true)
        lifecycleScope.launch {
            try {
                val o = Api.order(orderId)
                current = o
                b.toolbar.title = o.number.ifBlank { "Заказ №${o.id}" }
                b.orderTitle.text = o.number.ifBlank { "Заказ №${o.id}" }
                b.orderSubline.text = "Статус: " + OrdersAdapter.statusRu(o.status)
                b.orderDevice.text = o.deviceDescription?.trim().takeUnless { it.isNullOrEmpty() } ?: "—"
                b.orderEstimate.text = if (o.total > 0)
                    "Примерная стоимость ремонта: " + OrdersAdapter.money(o.total)
                else
                    "Примерная стоимость: не задана"
                renderPhotos(o.photoUrls)
            } catch (e: Exception) {
                Toast.makeText(this@OrderActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    // ── Фото по заказу ────────────────────────────────────────

    private fun renderPhotos(urls: List<String>) {
        photosAdapter.submit(urls)
        b.photosEmpty.visibility = if (urls.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun reloadPhotos() {
        lifecycleScope.launch {
            val o = runCatching { Api.order(orderId) }.getOrNull()
            if (o != null) { current = o; renderPhotos(o.photoUrls) }
        }
    }

    private fun launchCamera() {
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        pendingCameraUri = uri
        try {
            takePicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadFromUri(uri: Uri) {
        val raw = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (raw == null || raw.isEmpty()) {
            Toast.makeText(this, "Не удалось прочитать фото", Toast.LENGTH_SHORT).show()
            return
        }
        val bytes = downscaleJpeg(raw)
        setBusy(true)
        lifecycleScope.launch {
            try {
                Api.uploadOrderPhoto(orderId, bytes, "photo_${System.currentTimeMillis()}.jpg")
                b.photoCaption.text?.clear()
                Toast.makeText(this@OrderActivity, "Фото добавлено к заказу", Toast.LENGTH_SHORT).show()
                reloadPhotos()
            } catch (e: Exception) {
                Toast.makeText(this@OrderActivity, e.message ?: "Ошибка загрузки", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    /** Ужать до ~1600px по большей стороне и пережать в JPEG. */
    private fun downscaleJpeg(input: ByteArray): ByteArray {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
            val max = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sample = 1
            while (max / sample > 1600) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeByteArray(input, 0, input.size, opts) ?: return input
            ByteArrayOutputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            input
        }
    }

    private fun sharePhoto(url: String) {
        setBusy(true)
        lifecycleScope.launch {
            try {
                val bytes = Api.downloadBytes(Api.absoluteUrl(url))
                if (bytes.isEmpty()) throw RuntimeException("Пустой файл")
                val dir = File(cacheDir, "share").apply { mkdirs() }
                val file = File(dir, "photo_${url.hashCode()}.jpg").apply { writeBytes(bytes) }
                val uri = FileProvider.getUriForFile(this@OrderActivity, "$packageName.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(send, "Отправить фото"))
            } catch (e: Exception) {
                Toast.makeText(this@OrderActivity, e.message ?: "Не удалось поделиться", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    private fun confirmDeletePhoto(url: String) {
        AlertDialog.Builder(this)
            .setMessage("Удалить фото из заказа?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    runCatching { Api.deleteOrderAttachment(orderId, url) }
                        .onFailure { Toast.makeText(this@OrderActivity, it.message, Toast.LENGTH_SHORT).show() }
                    reloadPhotos()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openFullscreen(url: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val iv = ImageView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt())
            setOnClickListener { dialog.dismiss() }
        }
        iv.load(Api.absoluteUrl(url))
        dialog.setContentView(iv)
        dialog.show()
    }

    // ── Акт приёмки ───────────────────────────────────────────

    private fun fetchActFile(onReady: (File) -> Unit) {
        setBusy(true)
        lifecycleScope.launch {
            try {
                val bytes = Api.acceptanceActPdf(orderId)
                if (bytes.isEmpty()) throw RuntimeException("Пустой ответ сервера")
                val dir = File(cacheDir, "acts").apply { mkdirs() }
                val safeNum = (current?.number ?: orderId.toString()).replace(Regex("[^A-Za-z0-9А-Яа-я._-]"), "_")
                val file = File(dir, "act_$safeNum.pdf").apply { writeBytes(bytes) }
                onReady(file)
            } catch (e: Exception) {
                Toast.makeText(this@OrderActivity, e.message ?: "Не удалось получить акт", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    private fun printAct() = fetchActFile { file ->
        try {
            val pm = getSystemService(PrintManager::class.java)
                ?: throw RuntimeException("Служба печати недоступна")
            val name = "Акт приёмки ${current?.number ?: orderId}"
            pm.print(
                name,
                PdfPrintDocumentAdapter(file, name),
                PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Печать недоступна: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareAct() = fetchActFile { file ->
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Акт приёмки ${current?.number ?: orderId}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Отправить акт"))
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Не удалось отправить", Toast.LENGTH_LONG).show()
        }
    }

    // ── Редактирование ────────────────────────────────────────

    private fun editDialog() {
        val pad = (resources.displayMetrics.density * 20).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        val problem = EditText(this).apply {
            hint = "Неисправность / что принято"
            setText(current?.deviceDescription.orEmpty())
        }
        val price = EditText(this).apply {
            hint = "Примерная стоимость, ₽"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            if ((current?.total ?: 0.0) > 0) setText((current!!.total).let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            })
        }
        box.addView(problem)
        box.addView(price)

        AlertDialog.Builder(this)
            .setTitle("Изменить заказ")
            .setView(box)
            .setPositiveButton("Сохранить") { _, _ ->
                val p = problem.text?.toString()?.trim()
                val amount = price.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
                lifecycleScope.launch {
                    try {
                        Api.updateOrder(orderId, p, amount)
                        load()
                    } catch (e: Exception) {
                        Toast.makeText(this@OrderActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setBusy(busy: Boolean) {
        b.progress.visibility = if (busy) View.VISIBLE else View.GONE
        b.printActButton.isEnabled = !busy
        b.shareActButton.isEnabled = !busy
        b.editButton.isEnabled = !busy
        b.cameraButton.isEnabled = !busy
        b.galleryButton.isEnabled = !busy
    }

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_CLIENT_ID = "client_id"
        private const val STATE_CAMERA_URI = "pending_camera_uri"
    }
}
