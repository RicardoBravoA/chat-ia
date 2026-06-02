package com.bank.mobile

import com.bank.mobile.domain.model.PaymentReceiptUi

expect object GallerySaver {
    /** Guarda una imagen PNG del comprobante en la galería del usuario (Fotos / Pictures). */
    suspend fun savePaymentReceiptImage(receipt: PaymentReceiptUi): Result<Unit>
}
