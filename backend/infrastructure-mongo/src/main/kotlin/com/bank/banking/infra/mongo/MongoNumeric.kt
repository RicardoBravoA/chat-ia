package com.bank.banking.infra.mongo

import org.bson.Document
import org.bson.types.Decimal128

internal fun Document.getNumericDouble(key: String): Double? {
    val v = get(key) ?: return null
    return when (v) {
        is Number -> v.toDouble()
        is Decimal128 -> v.bigDecimalValue().toDouble()
        else -> null
    }
}

internal fun Document.getInt(key: String, default: Int): Int {
    val v = get(key) ?: return default
    return when (v) {
        is Number -> v.toInt()
        else -> default
    }
}
