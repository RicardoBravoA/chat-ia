package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.port.IntentClassifierPort

/**
 * Clasificador por reglas cargadas desde [local/config/intent_heuristic.json](local/config/intent_heuristic.json)
 * (véase [LocalIntentHeuristicLoader]).
 */
class HeuristicIntentClassifier(
    private val config: IntentHeuristicFile,
) : IntentClassifierPort {

    private val amountRegex: Regex =
        config.amountCaptureRegex?.let { Regex(it) } ?: Regex("""(\d+(?:\.\d+)?)""")

    override suspend fun classify(message: String): IntentClassification {
        val text = message.lowercase()
        val entities = mutableMapOf<String, String>()
        amountRegex.find(text)?.groupValues?.getOrNull(1)?.let { entities["amount"] = it }

        var intent = IntentLabel.AMBIGUOUS
        var confidence = config.defaultBaselineConfidence
        val reason = config.reason

        for (rule in config.rules) {
            if (!matches(rule.match, text)) continue
            intent = IntentLabel.fromClassifierOutput(rule.intent)
            confidence = rule.confidence
            for (cond in rule.conditionalEntities) {
                if (cond.whenSubstring.lowercase() in text) {
                    for ((k, v) in cond.set) {
                        if (k == "beneficiaryName") {
                            entities.putIfAbsent(k, v)
                        } else {
                            entities[k] = v
                        }
                    }
                }
            }
            break
        }

        val clarificationNeeded = intent == IntentLabel.AMBIGUOUS || confidence < 0.65
        return IntentClassification(
            intent = intent,
            confidence = confidence,
            entities = entities,
            clarificationNeeded = clarificationNeeded,
            reason = reason,
            source = "heuristic",
        )
    }

    private fun matches(spec: HeuristicMatch, text: String): Boolean {
        return when (spec.kind.lowercase()) {
            "anysubstring" -> spec.values.any { it.lowercase() in text }
            "anyphrase" -> spec.values.any { it.lowercase() in text }
            else -> false
        }
    }
}
