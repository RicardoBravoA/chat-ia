package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.port.IntentClassifierPort
import java.nio.file.Files
import java.nio.file.Paths

object IntentClassifierFactory {
    /**
     * - [INTENT_ROUTER_MODE]: `auto` (defecto), `python`, `heuristic`.
     * - [REPO_ROOT]: raíz del repo `ia/` (donde están `local/scripts` y `local/models`). Obligatorio si mode=python.
     * - [PYTHON_BIN]: por defecto `python3`.
     * - [INTENT_LOCAL_MIN_CONFIDENCE_FOR_ACCEPT]: umbral de aceptación local en auto (default 0.75).
     */
    fun createFromEnvironment(): IntentClassifierPort {
        val heuristic = HeuristicIntentClassifier(LocalIntentHeuristicLoader.load())
        val mode = (System.getenv("INTENT_ROUTER_MODE") ?: "auto").lowercase()
        val repoRootEnv = System.getenv("REPO_ROOT")?.trim()?.takeIf { it.isNotEmpty() }
        val repoRoot = repoRootEnv?.let { Paths.get(it) }
        val pythonBin = System.getenv("PYTHON_BIN")?.trim()?.takeIf { it.isNotEmpty() } ?: "python3"
        val localMinConfidenceForAccept =
            System.getenv("INTENT_LOCAL_MIN_CONFIDENCE_FOR_ACCEPT")
                ?.trim()
                ?.toDoubleOrNull()
                ?.coerceIn(0.0, 1.0)
                ?: 0.75

        val modelPath = repoRoot?.resolve("local/models/intent_tfidf_svc.joblib")
        val scriptPath = repoRoot?.resolve("local/scripts/predict_intent.py")
        val pythonReady =
            repoRoot != null &&
                scriptPath != null &&
                Files.isRegularFile(scriptPath) &&
                modelPath != null &&
                Files.isRegularFile(modelPath)

        val pythonClassifier =
            if (repoRoot != null && scriptPath != null && Files.isRegularFile(scriptPath)) {
                PythonScriptIntentClassifier(repoRoot, pythonBin, scriptPath)
            } else {
                null
            }

        return when (mode) {
            "heuristic" -> heuristic
            "python" -> {
                require(pythonClassifier != null && pythonReady) {
                    "INTENT_ROUTER_MODE=python requires REPO_ROOT, local/scripts/predict_intent.py and local/models/intent_tfidf_svc.joblib"
                }
                pythonClassifier
            }
            else -> {
                val localFirstClassifier = pythonClassifier?.takeIf { pythonReady }
                if (localFirstClassifier != null) {
                    FallbackIntentClassifier(
                        primary = localFirstClassifier,
                        fallback = heuristic,
                        fallbackOnResult = { localResult ->
                            shouldEscalateToFallback(localResult, localMinConfidenceForAccept)
                        },
                    )
                } else {
                    heuristic
                }
            }
        }
    }
}

private fun shouldEscalateToFallback(
    localResult: IntentClassification,
    minConfidence: Double,
): Boolean {
    return localResult.clarificationNeeded ||
        localResult.intent == IntentLabel.AMBIGUOUS ||
        localResult.confidence < minConfidence
}
