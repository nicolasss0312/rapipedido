package com.rapipedido.eventos;

/** La pasarela aprobó el cobro del pedido. */
public record PagoAprobado(Long pedidoId, Long pagoId, String referencia) {
}
