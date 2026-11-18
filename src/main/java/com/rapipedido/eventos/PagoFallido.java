package com.rapipedido.eventos;

/** El cobro fue rechazado o la pasarela no respondió tras agotar los reintentos. */
public record PagoFallido(Long pedidoId, Long pagoId, String motivo) {
}
