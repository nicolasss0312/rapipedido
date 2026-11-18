package com.rapipedido.eventos;

/** El repartidor recogió el pedido en el restaurante. */
public record PedidoRecogido(Long pedidoId, Long repartidorId) {
}
