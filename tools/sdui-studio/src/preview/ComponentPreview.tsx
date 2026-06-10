import type { ReactNode } from "react";
import { getComponent } from "../catalog";
import type { StudioNode, UiActionDef } from "../catalog/types";
import { isContainerCatalogId } from "../lib/container";

export type PreviewVariant = "full" | "thumb";

interface ComponentPreviewProps {
  catalogId: string;
  props?: Record<string, string>;
  actions?: UiActionDef[];
  variant?: PreviewVariant;
}

export function ComponentPreview({
  catalogId,
  props: propsOverride,
  actions: actionsOverride,
  variant = "full",
}: ComponentPreviewProps) {
  const def = getComponent(catalogId);
  if (!def) return <div className="preview-fallback">Desconocido: {catalogId}</div>;

  const props = propsOverride ?? def.defaultProps;
  const actions = actionsOverride ?? def.defaultActions ?? [];

  const wrap = (content: ReactNode) => (
    <div className={variant === "thumb" ? "preview-thumb-inner" : undefined}>{content}</div>
  );

  switch (catalogId) {
    case "Column":
      return wrap(
        <div className="layout-placeholder layout-column-preview">
          <span>Column ↓</span>
          <small>Apila hijos en vertical</small>
        </div>,
      );
    case "Row":
      return wrap(
        <div className="layout-placeholder layout-row-preview">
          <span>Row →</span>
          <small>Coloca hijos en horizontal</small>
        </div>,
      );
    case "AssistantText":
      return wrap(
        <div className="assistant-bubble">
          <p>{props.text || "…"}</p>
          {variant === "full" && <span className="bubble-time">10:30 AM</span>}
        </div>,
      );
    case "BalanceCard":
      return wrap(
        <div className="balance-card">
          <span className="balance-label">AVAILABLE BALANCE</span>
          <span className="balance-amount">{props.amountFormatted}</span>
        </div>,
      );
    case "PayCardPanel":
      return wrap(<PayCardPanelPreview props={props} compact={variant === "thumb"} />);
    case "SupportChannelsCard":
      return wrap(<SupportChannelsPreview compact={variant === "thumb"} />);
    case "InfoBanner":
      return wrap(<div className="info-banner">{props.text}</div>);
    case "GreetingCard":
      return wrap(<GreetingCardPreview message={props.message} actions={actions} compact={variant === "thumb"} />);
    case "ChatHistoryRow":
      return wrap(
        <div className="history-row">
          <div className="history-row-top">
            <span className="history-label">{props.sessionLabel}</span>
            {variant === "full" && <span className="history-time">{props.timeLabel}</span>}
          </div>
          <p>{props.lastMessage}</p>
          {variant === "full" && (
            <small>
              {props.turnCount} mensajes · {props.lastIntent}
            </small>
          )}
        </div>,
      );
    case "SpendingCategoryRow":
      return wrap(
        <div className="spending-row">
          <div>
            <strong>{props.category}</strong>
            <small>{props.transactionCount} trans.</small>
          </div>
          <span className="spending-amount">{props.totalAmountFormatted}</span>
        </div>,
      );
    case "PaymentReceiptCard":
      return wrap(
        <div className="receipt-card">
          <div className="receipt-header">Pago exitoso</div>
          <p className="receipt-amount">
            {props.amountPaid} {props.currency}
          </p>
          {variant === "full" && <small>Ref: {props.movementId}</small>}
        </div>,
      );
    case "HomeBalanceCard":
      return wrap(
        <div className="balance-card home-balance">
          <span className="balance-label">SALDO DISPONIBLE</span>
          <span className="balance-amount">{props.balanceLabel}</span>
        </div>,
      );
    case "HomeTopBar":
      return wrap(<div className="home-topbar">{props.greeting}</div>);
    case "PaymentHistoryCard":
      return wrap(
        <div className="payment-history">
          <div>
            <strong>{props.title}</strong>
            <small>{props.dateLabel}</small>
          </div>
          <span>{props.amount}</span>
        </div>,
      );
    case "LoginFormCard":
      return wrap(
        <div className="login-card">
          <h3>{props.title}</h3>
          {variant === "full" && (
            <>
              <LoginEmailFieldPreview />
              <LoginPasswordFieldPreview />
              <button type="button" className="btn-primary">
                Ingresar
              </button>
            </>
          )}
        </div>,
      );
    case "LoginEmailField":
      return wrap(
        <LoginEmailFieldPreview label={props.label} placeholder={props.placeholder} />,
      );
    case "LoginPasswordField":
      return wrap(<LoginPasswordFieldPreview label={props.label} />);
    case "LoginSubmitButton":
      return wrap(
        <button type="button" className="btn-primary full">
          {props.label}
        </button>,
      );
    case "ChatAssistantHeader":
      return wrap(
        <div className="chat-header">
          <span className="online-dot" />
          {props.title}
        </div>,
      );
    case "TypingIndicator":
      return wrap(
        <div className="typing-dots">
          <span />
          <span />
          <span />
        </div>,
      );
    case "CreditCardChatVisual":
      return wrap(
        <div className="cc-visual">
          <span>{props.alias}</span>
          <span>•••• {props.lastFourDigits}</span>
        </div>,
      );
    case "PaymentModeOptionBox":
      return wrap(
        <div className="payment-mode-box selected">
          <small>{props.label}</small>
          <strong>{props.amount}</strong>
        </div>,
      );
    case "HomeMovementsSectionHeader":
      return wrap(<h4 className="section-header">{props.title}</h4>);
    default:
      return wrap(
        <div className="preview-fallback">{def.label}</div>,
      );
  }
}

/** Preview desde nodo del canvas (props editadas). */
export function NodePreview({
  node,
  variant = "full",
}: {
  node: StudioNode;
  variant?: PreviewVariant;
}) {
  if (isContainerCatalogId(node.catalogId)) {
    const layoutClass = node.catalogId === "Row" ? "preview-row" : "preview-column";
    return (
      <div className={layoutClass}>
        {node.children.map((child) => (
          <NodePreview key={child.instanceId} node={child} variant={variant} />
        ))}
      </div>
    );
  }
  return (
    <ComponentPreview
      catalogId={node.catalogId}
      props={node.props}
      actions={node.actions}
      variant={variant}
    />
  );
}

function PayCardPanelPreview({
  props,
  compact,
}: {
  props: Record<string, string>;
  compact?: boolean;
}) {
  const debt = props.debt ?? "0";
  const currency = props.currency ?? "PEN";
  const min = props.minimumPaymentDue ?? "0";
  const fmt = (n: string) =>
    `${currency === "PEN" ? "S/ " : ""}${Number(n).toLocaleString("es-PE", { minimumFractionDigits: 2 })}`;

  return (
    <div className={`pay-panel ${compact ? "compact" : ""}`}>
      <div className="cc-visual">
        <span>{props.alias}</span>
        <span>•••• {props.lastFourDigits}</span>
      </div>
      {!compact && (
        <>
          <small className="debt-label">DEUDA ACTUAL</small>
          <p className="debt-amount">{fmt(debt)}</p>
        </>
      )}
      <div className="payment-modes">
        <div className="payment-mode-box">
          <small>MÍNIMO</small>
          <strong>{fmt(min)}</strong>
        </div>
        <div className="payment-mode-box selected">
          <small>TOTAL</small>
          <strong>{fmt(debt)}</strong>
        </div>
      </div>
      {!compact && (
        <button type="button" className="btn-pay">
          Pagar ahora
        </button>
      )}
    </div>
  );
}

function SupportChannelsPreview({ compact }: { compact?: boolean }) {
  const channels = compact ? ["Web"] : ["Web", "Llamada", "WhatsApp"];
  return (
    <div className="assistant-bubble support-card">
      {!compact && <p>Esa función no está disponible. Canales de soporte:</p>}
      {channels.map((ch) => (
        <button key={ch} type="button" className="support-channel">
          {ch} →
        </button>
      ))}
    </div>
  );
}

function GreetingCardPreview({
  message,
  actions,
  compact,
}: {
  message?: string;
  actions: UiActionDef[];
  compact?: boolean;
}) {
  return (
    <div className="assistant-bubble">
      <p>{compact && message && message.length > 40 ? `${message.slice(0, 40)}…` : message}</p>
      <div className="quick-replies">
        {(compact ? actions.slice(0, 2) : actions).map((a) => (
          <span key={a.id} className="chip">
            {a.label}
          </span>
        ))}
      </div>
    </div>
  );
}

function LoginEmailFieldPreview({
  label = "Correo",
  placeholder = "tu@email.com",
}: {
  label?: string;
  placeholder?: string;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type="email" placeholder={placeholder} readOnly />
    </label>
  );
}

function LoginPasswordFieldPreview({ label = "Contraseña" }: { label?: string }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type="password" placeholder="••••••••" readOnly />
    </label>
  );
}
