package com.bank.mobile

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.bank.mobile.domain.model.PaymentReceiptUi
import com.bank.mobile.presentation.ui.atoms.formatMoney
import com.bank.mobile.presentation.ui.atoms.formatReceiptDate
import com.bank.mobile.presentation.ui.atoms.formatReceiptReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val W = 1080
private const val H = 1400

actual object GallerySaver {
    actual suspend fun savePaymentReceiptImage(receipt: PaymentReceiptUi): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = renderReceiptBitmap(receipt)
                val resolver = AndroidContextHolder.applicationContext.contentResolver
                val fileName = "comprobante_${receipt.movementId}_${receipt.occurredAtEpochMs}.png"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BankApp")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = resolver.insert(collection, values)
                    ?: error("No se pudo crear el archivo en la galería")
                resolver.openOutputStream(uri, "w")?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        error("Error al comprimir la imagen")
                    }
                } ?: error("No se pudo escribir la imagen")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            }
        }

    private fun renderReceiptBitmap(receipt: PaymentReceiptUi): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val navy = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1B2A.toInt() }
        val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D77FF.toInt() }
        val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 36f
        }
        val darkSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 28f
        }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 26f
        }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 52f
            isFakeBoldText = true
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3D77FF.toInt()
            textSize = 56f
            isFakeBoldText = true
        }
        val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF22C55E.toInt() }
        val whiteFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 64f
            isFakeBoldText = true
        }

        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), white)
        canvas.drawRect(0f, 0f, W.toFloat(), 32f, navy)

        val cx = W / 2f
        val cy = 160f
        canvas.drawCircle(cx, cy, 72f, green)
        canvas.drawText("✓", cx - 28f, cy + 28f, whiteFg)

        var y = 300f
        canvas.drawText("Pago Exitoso", cx - title.measureText("Pago Exitoso") / 2f, y, title)
        y += 70f
        val sub = "COMPROBANTE DE OPERACIÓN"
        darkSmall.textSize = 30f
        canvas.drawText(sub, cx - darkSmall.measureText(sub) / 2f, y, darkSmall)
        y += 60f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            strokeWidth = 4f
        }
        canvas.drawLine(80f, y, W - 80f, y, linePaint)
        y += 50f
        canvas.drawText("MONTO PAGADO", 80f, y, darkSmall)
        val amt = formatMoney(receipt.amountPaid, receipt.currency)
        canvas.drawText(amt, W - 80f - amountPaint.measureText(amt), y, amountPaint)
        y += 90f
        canvas.drawText("REFERENCIA", 80f, y, muted)
        canvas.drawText("FECHA", W / 2f + 40f, y, muted)
        y += 45f
        val ref = formatReceiptReference(receipt.movementId)
        val dateStr = formatReceiptDate(receipt.occurredAtEpochMs)
        canvas.drawText(ref, 80f, y, dark)
        canvas.drawText(dateStr, W / 2f + 40f, y, dark)

        y = H - 120f
        muted.textSize = 24f
        val foot = "Bank App • comprobante de pago"
        canvas.drawText(foot, cx - muted.measureText(foot) / 2f, y, muted)

        return bmp
    }
}
