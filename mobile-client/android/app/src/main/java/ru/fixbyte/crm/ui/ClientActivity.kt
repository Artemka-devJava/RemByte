package ru.fixbyte.crm.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
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
import ru.fixbyte.crm.ClientDetail
import ru.fixbyte.crm.ClientPhoto
import ru.fixbyte.crm.databinding.ActivityClientBinding
import java.io.ByteArrayOutputStream
import java.io.File

class ClientActivity : AppCompatActivity() {

    private lateinit var b: ActivityClientBinding
    private lateinit var photosAdapter: PhotosAdapter
    private var clientId: Long = 0
    private var detail: ClientDetail? = null
    private var pendingCameraUri: Uri? = null

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok) pendingCameraUri?.let { uploadFromUri(it, "camera.jpg", "image/jpeg") }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val mime = contentResolver.getType(it) ?: "image/jpeg"
                uploadFromUri(it, "photo.jpg", mime)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityClientBinding.inflate(layoutInflater)
        setContentView(b.root)

        clientId = intent.getLongExtra(ClientsActivity.EXTRA_CLIENT_ID, 0)
        if (clientId == 0L) { finish(); return }

        b.toolbar.setNavigationOnClickListener { finish() }

        photosAdapter = PhotosAdapter(
            onOpen = ::openFullscreen,
            onShare = ::sharePhoto,
            onDelete = ::confirmDelete
        )
        b.photosGrid.layoutManager = GridLayoutManager(this, 3)
        b.photosGrid.adapter = photosAdapter

        b.cameraButton.setOnClickListener { launchCamera() }
        b.galleryButton.setOnClickListener { pickImage.launch("image/*") }
        b.callButton.setOnClickListener { dial() }
        b.writeButton.setOnClickListener { write() }

        loadAll()
    }

    private fun loadAll() {
        setBusy(true)
        lifecycleScope.launch {
            try {
                val d = Api.client(clientId)
                detail = d
                b.toolbar.title = d.name
                b.name.text = if (d.archived) "${d.name}  · архив" else d.name
                b.subline.text = listOfNotNull(
                    d.phone.ifBlank { null },
                    if (d.type == "COMPANY") "Организация" else "Физлицо"
                ).joinToString("  ·  ")

                val s = runCatching { Api.summary(clientId) }.getOrNull()
                b.summary.text = if (s == null) "" else
                    "Заказов: ${s.ordersCount}     Оборот: ${fmt(s.revenue)}     Долг: ${fmt(s.debt)}"
                b.summary.visibility = if (s == null) View.GONE else View.VISIBLE

                b.details.text = buildString {
                    d.email?.let { append("Email: ").append(it).append('\n') }
                    d.address?.let { append("Адрес: ").append(it).append('\n') }
                    d.tags?.let { append("Метки: ").append(it).append('\n') }
                    d.notes?.let { append("Заметка: ").append(it) }
                }.trim()
                b.details.visibility = if (b.details.text.isBlank()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@ClientActivity, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                if (e.message?.contains("Сессия истекла") == true) toLogin()
            } finally {
                setBusy(false)
            }
            loadPhotos()
        }
    }

    private fun loadPhotos() {
        lifecycleScope.launch {
            val list = runCatching { Api.photos(clientId) }.getOrDefault(emptyList())
            photosAdapter.submit(list)
            b.photosEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ── Фото ──────────────────────────────────────────────────

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

    private fun uploadFromUri(uri: Uri, name: String, mime: String) {
        val raw = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (raw == null || raw.isEmpty()) {
            Toast.makeText(this, "Не удалось прочитать фото", Toast.LENGTH_SHORT).show()
            return
        }
        val bytes = downscaleJpeg(raw)
        val caption = b.photoCaption.text?.toString()?.trim()

        setBusy(true)
        lifecycleScope.launch {
            try {
                Api.uploadPhoto(clientId, bytes, if (name.endsWith(".jpg")) name else "$name.jpg", "image/jpeg", caption)
                b.photoCaption.text?.clear()
                Toast.makeText(this@ClientActivity, "Фото загружено", Toast.LENGTH_SHORT).show()
                loadPhotos()
            } catch (e: Exception) {
                Toast.makeText(this@ClientActivity, e.message ?: "Ошибка загрузки", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    /** Ужать до ~1600px по большей стороне и пережать в JPEG — быстрее и не упрётся в лимит 15 МБ. */
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
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            input
        }
    }

    /** Скачать фото из CRM и отдать в системный «Поделиться» (WhatsApp, Telegram, VK, почта…). */
    private fun sharePhoto(p: ClientPhoto) {
        setBusy(true)
        lifecycleScope.launch {
            try {
                val bytes = Api.downloadBytes(Api.absoluteUrl(p.url))
                if (bytes.isEmpty()) throw RuntimeException("Пустой файл")
                val dir = File(cacheDir, "share").apply { mkdirs() }
                val file = File(dir, "photo_${p.id}.jpg").apply { writeBytes(bytes) }
                val uri = FileProvider.getUriForFile(this@ClientActivity, "$packageName.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    if (!p.caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, p.caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(send, "Отправить фото"))
            } catch (e: Exception) {
                Toast.makeText(this@ClientActivity, e.message ?: "Не удалось поделиться", Toast.LENGTH_LONG).show()
            } finally {
                setBusy(false)
            }
        }
    }

    private fun confirmDelete(p: ClientPhoto) {
        AlertDialog.Builder(this)
            .setMessage("Удалить фото?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    runCatching { Api.deletePhoto(clientId, p.id) }
                        .onFailure { Toast.makeText(this@ClientActivity, it.message, Toast.LENGTH_SHORT).show() }
                    loadPhotos()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openFullscreen(p: ClientPhoto) {
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
        iv.load(Api.absoluteUrl(p.url))
        dialog.setContentView(iv)
        dialog.show()
    }

    // ── Связь ─────────────────────────────────────────────────

    private fun dial() {
        val d = detail ?: return
        val digits = d.phone.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) return
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    }

    private fun write() {
        val d = detail ?: return
        val digits = d.phone.filter { it.isDigit() }
        val email = d.email
        val intent: Intent? = when {
            d.preferredChannel == "TELEGRAM" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+$digits"))
            d.preferredChannel == "WHATSAPP" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
            !email.isNullOrBlank() -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
            digits.isNotBlank() -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
            else -> null
        }
        if (intent != null) runCatching { startActivity(intent) }
    }

    // ── Прочее ────────────────────────────────────────────────

    private fun setBusy(busy: Boolean) {
        b.progress.visibility = if (busy) View.VISIBLE else View.GONE
        b.cameraButton.isEnabled = !busy
        b.galleryButton.isEnabled = !busy
    }

    private fun fmt(v: Double) = "${v.toLong()}₽"

    private fun toLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
