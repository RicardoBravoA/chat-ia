package com.bank.banking.domain.model

data class IntentClassification(
    val intent: IntentLabel,
    val confidence: Double,
    val entities: Map<String, String>,
    val clarificationNeeded: Boolean,
    val reason: String,
    /** Origen: p. ej. "woz" o "heuristic". */
    val source: String,
)
