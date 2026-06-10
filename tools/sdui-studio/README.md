# SDUI Studio

Herramienta web interna para **componer UI del chat bancario**, previsualizar componentes mobile (atomic design) y **exportar `uiTree` JSON** compatible con `SduiRenderer` en Kotlin.

## Arranque

Desde la raíz del repo:

```bash
cd tools/sdui-studio
npm install
npm run dev
```

Abre [http://localhost:5174](http://localhost:5174).

Build de producción:

```bash
npm run build
npm run preview
```

Tests unitarios (Vitest):

```bash
npm test
npm run test:watch   # modo interactivo
```

## Qué incluye (fase inicial)

| Área | Componentes |
|------|-------------|
| **SDUI exportable** | `Column`, `Row`, `AssistantText`, `BalanceCard`, `PayCardPanel`, `SupportChannelsCard`, `InfoBanner`, `GreetingCard`, `ChatHistoryRow`, `SpendingCategoryRow` |
| **App UI (preview)** | Login, Home, chat header, typing, recibo, etc. — ver `src/catalog/catalog.json` |

- **Paleta / Galería** (izquierda): **clic** añade al `Column`/`Row` seleccionado; **+** al final del canvas raíz; **⠿** arrastra al área punteada del contenedor.
- **Canvas + Preview** (centro): árbol SDUI y vista móvil en vivo, lado a lado.
- **Inspector** (drawer): aparece al pulsar **Editar** o doble clic en un nodo del canvas.
- **Canvas**: `Column` / `Row` aceptan hijos; reordenar hermanos del mismo layout con ⋮⋮.
- **Preview**: marco 320px con tokens `AppPalette`.
- **Export**: `ChatMessageResponse` (con `studioCanvas` = árbol completo del canvas), solo `uiTree` mobile, o pestaña **Canvas completo**. Metadata (`intent`, `userMessage`) se infiere del contenido. Componentes **App** (login, home) van a `studioCanvas`, no a `uiTree`.
- **Guía mobile**: Composable Kotlin, rama `SduiRenderer`, checklist al añadir types.

## Catálogo

Fuente de verdad: [`src/catalog/catalog.json`](src/catalog/catalog.json).

Al añadir un Composable nuevo en mobile que deba ser SDUI:

1. Actualizar `UiComponentType.kt` y `SduiRenderer.kt`.
2. Añadir entrada en `catalog.json`.
3. Añadir preview en `src/preview/PreviewRenderer.tsx`.

## Plantillas

- Pagar TC
- Consultar saldo
- Saludo (quick replies)
- Clarificación

## Relación con backend/mobile

| Export studio | Consumidor |
|---------------|------------|
| `uiTree` JSON | `BuildChatUiUseCase` / builders en `backend/application/sdui/` |
| Mismo JSON | `ChatViewModel` → `ChatBubble.sduiRoot` → `SduiRenderer` |

Componentes marcados **App** en la paleta no forman parte del contrato SDUI v1; se muestran para diseño y documentación de pantallas Compose.

Ver también: [`docs/flujo-chat-pagar-tc.md`](../../docs/flujo-chat-pagar-tc.md), [`docs/server-driven-ui-blueprint.md`](../../docs/server-driven-ui-blueprint.md).
