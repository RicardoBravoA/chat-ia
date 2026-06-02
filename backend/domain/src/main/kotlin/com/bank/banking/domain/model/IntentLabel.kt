package com.bank.banking.domain.model

/**
 * Intenciones alineadas con `local/datasets/intents/README.md` y el clasificador TF-IDF.
 */
enum class IntentLabel {
    CHECK_BALANCE,
    PAY_CREDIT_CARD,
    TRANSFER_OWN_ACCOUNTS,
    TRANSFER_THIRD_PARTY,
    AMBIGUOUS,
    OUT_OF_SCOPE,
    ;

    companion object {
        fun fromClassifierOutput(raw: String): IntentLabel {
            val s = raw.trim().uppercase().replace('-', '_')
            return entries.find { it.name == s } ?: AMBIGUOUS
        }
    }
}
