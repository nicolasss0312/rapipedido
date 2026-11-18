package com.rapipedido.eventos;

/** Cualquier cambio de estado de un pedido; lo usa el módulo de notificaciones. */
public record EstadoPedidoCambiado(Long pedidoId, Long clienteId, Long restauranteId,
                                   String estadoAnterior, String estadoNuevo) {
}
