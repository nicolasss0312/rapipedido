package com.rapipedido.eventos;

/** Se asignó un repartidor al pedido. */
public record RepartidorAsignado(Long pedidoId, Long repartidorId) {
}
