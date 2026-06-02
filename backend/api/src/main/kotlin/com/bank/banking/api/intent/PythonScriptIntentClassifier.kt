package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.port.IntentClassifierPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Ejecuta `local/scripts/predict_intent.py` sobre el mismo [repoRoot] donde vive el modelo en `local/models/`.
 * Debe existir el artefacto entrenado (ver `train_intent_classifier.py`).
 */
class PythonScriptIntentClassifier(
    private val repoRoot: Path,
    private val pythonBinary: String,
    private val scriptPath: Path,
    private val timeoutSeconds: Long = 15L,
) : IntentClassifierPort {

    override suspend fun classify(message: String): IntentClassification =
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(
                pythonBinary,
                scriptPath.toAbsolutePath().toString(),
                message,
            )
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw IllegalStateException("predict_intent.py timed out")
            }
            if (process.exitValue() != 0) {
                val err = process.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                throw IllegalStateException("predict_intent.py exit ${process.exitValue()}: $err")
            }
            val raw = process.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }.trim()
            val label = IntentLabel.fromClassifierOutput(raw)
            IntentClassification(
                intent = label,
                confidence = 0.95,
                entities = emptyMap(),
                clarificationNeeded = label == IntentLabel.AMBIGUOUS,
                reason = "local TF-IDF model (predict_intent.py)",
                source = "python-joblib",
            )
        }
}
