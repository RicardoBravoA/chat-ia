package com.bank.mobile

import com.bank.mobile.domain.model.PaymentReceiptUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object GallerySaver {
    actual suspend fun savePaymentReceiptImage(receipt: PaymentReceiptUi): Result<Unit> =
        withContext(Dispatchers.Default) {
            Result.failure(
                UnsupportedOperationException(
                    "Guardar en la galería no está disponible en iOS en esta versión.",
                ),
            )
        }
}
