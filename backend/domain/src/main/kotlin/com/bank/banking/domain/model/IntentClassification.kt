package com.bank.banking.domain.model

data class IntentClassification(
    val intent: IntentLabel,
    val confidence: Double,
    val entities: Map<String, String>,
    val clarificationNeeded: Boolean,
    val reason: String,
    /** Origen: p. ej. "python-joblib" o "heuristic". */
    val source: String,
)
