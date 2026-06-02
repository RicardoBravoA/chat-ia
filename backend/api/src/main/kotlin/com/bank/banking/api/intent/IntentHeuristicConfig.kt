package com.bank.banking.api.intent

import kotlinx.serialization.Serializable

@Serializable
data class IntentHeuristicFile(
    val version: Int = 1,
    val description: String = "",
    val amountCaptureRegex: String? = null,
    val defaultBaselineConfidence: Double = 0.75,
    val reason: String = "heuristic-local-config",
    val rules: List<IntentHeuristicRule>,
)

@Serializable
data class IntentHeuristicRule(
    val intent: String,
    val confidence: Double,
    val match: HeuristicMatch,
    val conditionalEntities: List<ConditionalEntity> = emptyList(),
)

@Serializable
data class HeuristicMatch(
    /** `anySubstring` o `anyPhrase` */
    val kind: String,
    val values: List<String>,
)

@Serializable
data class ConditionalEntity(
    val whenSubstring: String,
    val set: Map<String, String>,
)
