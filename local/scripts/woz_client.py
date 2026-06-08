#!/usr/bin/env python3
"""
Cliente compartido para Woz — clasificador de intenciones vía LLM local (Ollama).

Variables de entorno:
  WOZ_OLLAMA_BASE_URL  default http://127.0.0.1:11434
  WOZ_MODEL            default qwen2.5:7b-instruct
  WOZ_TIMEOUT_SECONDS  default 60
"""

from __future__ import annotations

import json
import os
import re
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any, Dict, Optional


INTENTS = {
    "CHECK_BALANCE",
    "PAY_CREDIT_CARD",
    "TRANSFER_OWN_ACCOUNTS",
    "TRANSFER_THIRD_PARTY",
    "VIEW_CHAT_HISTORY",
    "MONTHLY_EXPENSES",
    "AMBIGUOUS",
    "OUT_OF_SCOPE",
}

WOZ_SYSTEM_PROMPT = """Eres Woz, copiloto bancario conversacional. Clasifica la intencion del usuario y extrae entidades.
Responde SOLO un objeto JSON valido con este schema exacto:
{"intent":"CHECK_BALANCE|PAY_CREDIT_CARD|TRANSFER_OWN_ACCOUNTS|TRANSFER_THIRD_PARTY|VIEW_CHAT_HISTORY|MONTHLY_EXPENSES|AMBIGUOUS|OUT_OF_SCOPE","confidence":0.0,"entities":{},"clarification_needed":true,"reason":"string corto"}
Reglas:
- Si no estas seguro, usa AMBIGUOUS y clarification_needed true.
- No inventes entidades ni montos.
- confidence entre 0 y 1.
- "tc" en contexto de pago suele ser tarjeta de credito (PAY_CREDIT_CARD).
- Si el mensaje mezcla saludo o cortesia con una operacion bancaria clara (ej. "hola, puedes ayudarme pagando mi tc"), clasifica la OPERACION (PAY_CREDIT_CARD, CHECK_BALANCE, etc.) con confidence >= 0.85 y clarification_needed false. No uses AMBIGUOUS solo por incluir "hola" o "ayudame".
- Formas verbales de pago cuentan igual: pagar, pagando, pago, abonar, abonando, liquidar, saldar + tc/tarjeta -> PAY_CREDIT_CARD.
- VIEW_CHAT_HISTORY: historial de conversaciones previas con el asistente.
- MONTHLY_EXPENSES: gastos del mes por categoria. entities opcional: yearMonth en formato YYYY-MM.
- OUT_OF_SCOPE solo si claramente fuera de operaciones bancarias del asistente.
- En el historial, los mensajes assistant son JSON compacto con intent, entities, confidence, reason y source del turno anterior. Usalo para resolver follow-ups como "paga 50" o "la oro".
"""


@dataclass
class WozPrediction:
    intent: str
    confidence: float
    entities: Dict[str, Any]
    clarification_needed: bool
    reason: str
    source: str = "woz"


def woz_config() -> Dict[str, Any]:
    base = (os.getenv("WOZ_OLLAMA_BASE_URL") or "http://127.0.0.1:11434").strip().rstrip("/")
    model = (os.getenv("WOZ_MODEL") or "qwen2.5:7b-instruct").strip()
    timeout = int(os.getenv("WOZ_TIMEOUT_SECONDS") or "60")
    return {"base_url": base, "model": model, "timeout": max(5, min(timeout, 300))}


def extract_first_json(text: str) -> Dict[str, Any]:
    match = re.search(r"\{.*\}", text, flags=re.DOTALL)
    if not match:
        raise ValueError(f"No JSON object in Woz response: {text[:300]}")
    return json.loads(match.group(0))


def normalize_prediction(data: Dict[str, Any]) -> WozPrediction:
    intent = str(data.get("intent", "AMBIGUOUS")).strip().upper()
    if intent not in INTENTS:
        intent = "AMBIGUOUS"
    confidence = float(data.get("confidence", 0.0))
    confidence = max(0.0, min(1.0, confidence))
    entities = data.get("entities", {})
    if not isinstance(entities, dict):
        entities = {}
    clarification_needed = bool(
        data.get("clarification_needed", intent == "AMBIGUOUS" or confidence < 0.55)
    )
    reason = str(data.get("reason", "Woz local LLM classification"))
    return WozPrediction(intent, confidence, entities, clarification_needed, reason)


def _ollama_http_hint(err_body: str) -> str:
    if "llama-server binary not found" in err_body:
        return (
            "\n\nSugerencia: Ollama instalado con Homebrew 0.30+ suele NO incluir llama-server "
            "(modelos GGUF como qwen2.5 fallan). Usa el instalador oficial:\n"
            "  brew uninstall ollama\n"
            "  curl -fsSL https://ollama.com/install.sh | sh\n"
            "  # o descarga https://ollama.com/download (app macOS)\n"
            "Luego: ollama pull qwen2.5:7b-instruct && python3 local/scripts/predict_intent_woz.py \"paga mi tc\""
        )
    if "model" in err_body.lower() and "not found" in err_body.lower():
        return "\n\nSugerencia: ejecuta `ollama pull qwen2.5:7b-instruct` (o el valor de WOZ_MODEL)."
    return ""


def classify_with_woz(
    user_text: str,
    *,
    history: Optional[list] = None,
    base_url: Optional[str] = None,
    model: Optional[str] = None,
    timeout: Optional[int] = None,
) -> WozPrediction:
    cfg = woz_config()
    base = (base_url or cfg["base_url"]).rstrip("/")
    model_name = model or cfg["model"]
    req_timeout = timeout or cfg["timeout"]
    messages = [{"role": "system", "content": WOZ_SYSTEM_PROMPT}]
    for turn in history or []:
        messages.append({"role": turn["role"], "content": turn["content"]})
    messages.append({"role": "user", "content": f"Texto usuario a clasificar: {user_text.strip()}"})
    payload = {
        "model": model_name,
        "messages": messages,
        "stream": False,
        "format": "json",
        "options": {"temperature": 0},
    }
    req = urllib.request.Request(
        f"{base}/api/chat",
        method="POST",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=req_timeout) as resp:
            body = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        hint = _ollama_http_hint(err)
        raise RuntimeError(f"Woz Ollama HTTP {e.code}: {err}{hint}") from e
    except urllib.error.URLError as e:
        raise RuntimeError(
            f"No se pudo conectar a Ollama en {base} ({e.reason}). "
            "Ejecuta: ollama serve && ollama pull qwen2.5:7b-instruct"
        ) from e
    content = body.get("message", {}).get("content", "")
    data = extract_first_json(content)
    return normalize_prediction(data)


def decide_next_action(intent: str, confidence: float, clarify_threshold: float = 0.55) -> str:
    if intent == "AMBIGUOUS" or confidence < clarify_threshold:
        return "ASK_CLARIFICATION"
    if intent == "CHECK_BALANCE":
        return "SHOW_BALANCE"
    if intent == "PAY_CREDIT_CARD":
        return "SHOW_PAY_CARD_OPTIONS"
    if intent in ("TRANSFER_OWN_ACCOUNTS", "TRANSFER_THIRD_PARTY"):
        return "SHOW_TRANSFER_GUIDE"
    if intent == "OUT_OF_SCOPE":
        return "SHOW_SUPPORT"
    return "GENERIC_REPLY"


def build_bot_response(intent: str, action: str) -> str:
    if action == "ASK_CLARIFICATION":
        return (
            "No me queda claro lo que necesitas. "
            "Puedes decirme si quieres consultar saldo, pagar tarjeta o hacer transferencia."
        )
    if action == "SHOW_BALANCE":
        return "Perfecto. Te ayudo con la consulta de saldo y movimientos recientes."
    if action == "SHOW_PAY_CARD_OPTIONS":
        return (
            "Entendido. Te ayudo con el pago de tarjeta. "
            "Indica la tarjeta y si deseas pago minimo, total o monto personalizado."
        )
    if action == "SHOW_TRANSFER_GUIDE":
        if intent == "TRANSFER_OWN_ACCOUNTS":
            return "Vamos con tu transferencia entre cuentas propias. Indica cuenta origen, destino y monto."
        return "Vamos con tu transferencia a terceros. Indica destinatario, banco y monto."
    if action == "SHOW_SUPPORT":
        return "Esto lo gestiona soporte. Te puedo derivar a web, telefono o WhatsApp."
    return "Te ayudo con eso. Dame un poco mas de detalle."
