package com.rapipedido.eventos;

/** El repartidor confirmó la entrega al cliente. */
public record EntregaConfirmada(Long pedidoId, Long repartidorId) {
}
