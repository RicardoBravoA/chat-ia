package com.bank.banking.domain.error

sealed class DomainException(message: String) : Exception(message)

class AuthenticationException(message: String = "Invalid credentials") : DomainException(message)
class UnauthorizedException(message: String = "Unauthorized") : DomainException(message)
class NotFoundException(message: String) : DomainException(message)
class ConflictException(message: String) : DomainException(message)
class InsufficientFundsException(message: String = "Insufficient funds") : DomainException(message)
class InvalidPaymentAmountException(message: String) : DomainException(message)
