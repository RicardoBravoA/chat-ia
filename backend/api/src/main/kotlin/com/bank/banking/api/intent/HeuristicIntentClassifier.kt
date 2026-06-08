package com.bank.banking.api.intent

import com.bank.banking.domain.model.ChatHistoryMessage
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

    override suspend fun classify(
        message: String,
        history: List<ChatHistoryMessage>,
    ): IntentClassification {
        val text = message.lowercase()
        val entities = mutableMapOf<String, String>()
        amountRegex.find(text)?.groupValues?.getOrNull(1)?.let { entities["amount"] = it }
        YEAR_MONTH_REGEX.find(text)?.value?.let { entities["yearMonth"] = it }
        NUMERIC_MONTH_YEAR_REGEX.find(text)?.let { match ->
            val month = match.groupValues[1].padStart(2, '0')
            val year = match.groupValues[2]
            entities["yearMonth"] = "$year-$month"
        }

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

        if (intent == IntentLabel.AMBIGUOUS) {
            BankingIntentKeywordResolver.resolveAmbiguous(text)?.let { resolved ->
                intent = resolved
                confidence = 0.88
            }
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

    companion object {
        private val YEAR_MONTH_REGEX = Regex("""20\d{2}-\d{2}""")
        private val NUMERIC_MONTH_YEAR_REGEX = Regex("""\b(0?[1-9]|1[0-2])[/-](20\d{2})\b""")
    }

    private fun matches(spec: HeuristicMatch, text: String): Boolean {
        return when (spec.kind.lowercase()) {
            "anysubstring" -> spec.values.any { it.lowercase() in text }
            "anyphrase" -> spec.values.any { it.lowercase() in text }
            else -> false
        }
    }
}
