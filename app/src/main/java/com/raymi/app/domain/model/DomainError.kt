package com.raymi.app.domain.model

sealed class DomainError(val message: String) {
    object InsufficientStock : DomainError("No hay suficiente stock disponible")
    object ItemNotAvailable : DomainError("El artículo no está disponible para alquiler")
    object NegativeBalance : DomainError("El saldo no puede ser negativo")
    object InvalidStateTransition : DomainError("Transición de estado no permitida")
    object BusinessRequired : DomainError("Se requiere un negocio activo para esta operación")
    object InvalidDateRange : DomainError("La fecha de fin debe ser posterior a la de inicio")
    class Unknown(message: String) : DomainError(message)
}
