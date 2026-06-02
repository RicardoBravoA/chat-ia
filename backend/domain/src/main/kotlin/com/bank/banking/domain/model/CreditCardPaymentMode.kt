package com.bank.banking.domain.model

/**
 * Tipo de pago sobre la tarjeta.
 *
 * - [MINIMUM]: pago mínimo exigido para no incurrir en mora (campo en BD).
 * - [STATEMENT_MONTH]: saldo o cuota asociada al cierre / estado de cuenta del periodo.
 * - [FULL_DEBT]: abona toda la deuda revolvente actual ([CreditCard.debt]).
 * - [CUSTOM]: monto libre indicado por el usuario (dentro de reglas de negocio).
 */
enum class CreditCardPaymentMode {
    MINIMUM,
    STATEMENT_MONTH,
    FULL_DEBT,
    CUSTOM,
}
