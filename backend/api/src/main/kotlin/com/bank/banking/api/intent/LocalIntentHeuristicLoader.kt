package com.bank.banking.api.intent

import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Carga [IntentHeuristicFile] desde `local/config/intent_heuristic.json` en el repo.
 *
 * Resolución (primera que exista como archivo regular):
 * 1. [INTENT_HEURISTIC_CONFIG] — ruta absoluta al JSON
 * 2. [REPO_ROOT]/local/config/intent_heuristic.json
 * 3. Subir directorios desde `user.dir` hasta encontrar `local/config/intent_heuristic.json`
 */
object LocalIntentHeuristicLoader {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun resolvePath(): Path? {
        System.getenv("INTENT_HEURISTIC_CONFIG")?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return Paths.get(it)
        }
        System.getenv("REPO_ROOT")?.trim()?.takeIf { it.isNotEmpty() }?.let {
            val p = Paths.get(it).resolve("local/config/intent_heuristic.json")
            if (Files.isRegularFile(p)) return p
        }
        var dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(12) {
            val p = dir.resolve("local/config/intent_heuristic.json")
            if (Files.isRegularFile(p)) return p
            dir = dir.parent ?: return null
        }
        return null
    }

    fun load(): IntentHeuristicFile {
        val path = resolvePath()
            ?: error(
                "No se encontró local/config/intent_heuristic.json. " +
                    "Define INTENT_HEURISTIC_CONFIG, REPO_ROOT, o ejecuta el servidor desde el repo (p. ej. carpeta backend/ o raíz ia/).",
            )
        val text = Files.readString(path)
        return json.decodeFromString<IntentHeuristicFile>(text)
    }
}
