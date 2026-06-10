import { getComponent } from "../catalog";
import { mobileRenderSnippet } from "../lib/exportUiTree";
import { useStudio } from "../store/StudioContext";

export function MobileGuide() {
  const { selectedNode } = useStudio();
  const def = selectedNode ? getComponent(selectedNode.catalogId) : null;

  return (
    <section className="mobile-guide">
      <h2>Render en mobile (Kotlin)</h2>
      {def ? (
        <>
          <dl className="guide-dl">
            <dt>Composable</dt>
            <dd>
              <code>mobile/composeApp/.../{def.composeFile}</code>
            </dd>
            <dt>Flujo SDUI</dt>
            <dd>
              <code>{mobileRenderSnippet(def)}</code>
            </dd>
            {def.sduiType && (
              <>
                <dt>type en JSON</dt>
                <dd>
                  <code>&quot;{def.sduiType}&quot;</code>
                </dd>
              </>
            )}
          </dl>

          {def.sduiExportable ? (
            <div className="guide-code">
              <p>En <code>ChatScreen.kt</code>:</p>
              <pre>{`if (!msg.isUser && msg.sduiRoot != null) {
    SduiRenderer(
        node = msg.sduiRoot,
        timestampEpochMs = msg.timestampEpochMs,
        payingCardId = payingCardId,
        paidCardIds = msg.paidCardIds,
        onPayCard = onPayCard,
        onQuickReply = onQuickReply,
    )
}`}</pre>
              <p>En <code>SduiRenderer.kt</code> (rama relevante):</p>
              <pre>{sduiBranch(def.sduiType!)}</pre>
            </div>
          ) : (
            <div className="guide-code">
              <p className="guide-note">
                Este componente <strong>no</strong> va en <code>uiTree</code> ni en{" "}
                <code>SduiRenderer</code>. Solo aparece en <code>studioCanvas</code> y en preview
                del studio.
              </p>
              <p>Render en mobile (pantalla Compose directa):</p>
              <pre>{appComposeSnippet(def.id, def.composeFile, def.mobileRender)}</pre>
            </div>
          )}
        </>
      ) : (
        <p className="panel-hint">
          Selecciona un componente para ver archivo Kotlin, rama en SduiRenderer y contrato
          props → Composable.
        </p>
      )}

      <div className="guide-checklist">
        <h3>Checklist al añadir un type SDUI nuevo</h3>
        <ol>
          <li>
            <code>UiComponentType.kt</code> — constante del type
          </li>
          <li>
            <code>SduiRenderer.kt</code> — rama <code>when</code>
          </li>
          <li>
            <code>application/sdui/*UiBuilder.kt</code> — backend arma el árbol
          </li>
          <li>
            <code>SduiNodeMapperTest.kt</code> — tests mobile
          </li>
          <li>
            <code>tools/sdui-studio/src/catalog/catalog.json</code> — esta paleta
          </li>
        </ol>
      </div>
    </section>
  );
}

function appComposeSnippet(
  catalogId: string,
  composeFile: string,
  mobileRender?: string,
): string {
  const path = `mobile/composeApp/src/commonMain/kotlin/com/bank/mobile/presentation/ui/`;
  const snippets: Record<string, string> = {
    LoginFormCard: `// LoginScreen.kt
LoginFormCard(
    title = props["title"],
    onSubmit = { ... },
    content = {
        LoginEmailField(...)
        LoginPasswordField(...)
        LoginSubmitButton(...)
    }
)`,
    LoginEmailField: `// Dentro de LoginFormCard.kt
LoginEmailField(
    label = "${catalogId === "LoginEmailField" ? "Correo" : "…"}",
    value = email,
    onValueChange = { ... },
)`,
    LoginPasswordField: `LoginPasswordField(
    label = "Contraseña",
    value = password,
    onValueChange = { ... },
)`,
    LoginSubmitButton: `LoginSubmitButton(
    label = "Ingresar",
    onClick = { ... },
)`,
  };
  const body = snippets[catalogId] ?? `// ${composeFile}\n// ${mobileRender ?? catalogId}`;
  return `${path}…/${composeFile}\n\n${body}`;
}

function sduiBranch(sduiType: string): string {
  const branches: Record<string, string> = {
    Column: `UiComponentType.COLUMN -> {
    Column { node.children.forEach { SduiRenderer(...) } }
}`,
    Row: `UiComponentType.ROW -> {
    Row { node.children.forEach { SduiRenderer(..., modifier = Modifier.weight(1f)) } }
}`,
    AssistantText: `UiComponentType.ASSISTANT_TEXT -> {
    AssistantMessageBubble(...) {
        Text(node.propText("text"))
    }
}`,
    BalanceCard: `UiComponentType.BALANCE_CARD -> {
    ChatBalanceMiniCard(amountLabel = node.propText("amountFormatted"))
}`,
    PayCardPanel: `UiComponentType.PAY_CARD_PANEL -> {
    val action = node.toPayCardChatAction()
    PayCardChatPanel(action = action, onPay = { ... })
}`,
    SupportChannelsCard: `UiComponentType.SUPPORT_CHANNELS_CARD -> {
    OutOfScopeSupportCard(...)
}`,
    InfoBanner: `UiComponentType.INFO_BANNER -> {
    Card { Text(node.propText("text")) }
}`,
    GreetingCard: `UiComponentType.GREETING_CARD -> {
    ChatGreetingCard(message = node.propText("message"), quickReplies = node.actions, ...)
}`,
    ChatHistoryRow: `UiComponentType.CHAT_HISTORY_ROW -> {
    ChatHistoryRowCard(sessionLabel = ..., lastMessage = ..., ...)
}`,
    SpendingCategoryRow: `UiComponentType.SPENDING_CATEGORY_ROW -> {
    SpendingCategoryRowCard(category = ..., transactionCount = ..., ...)
}`,
  };
  return branches[sduiType] ?? `// ${sduiType}`;
}
