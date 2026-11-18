package com.rapipedido.eventos;

/** El restaurante terminó el pedido; hay que asignar un repartidor cerca del restaurante. */
public record PedidoListo(Long pedidoId, Long restauranteId, double lat, double lng) {
}
