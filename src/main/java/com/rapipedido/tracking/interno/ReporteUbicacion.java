package com.rapipedido.tracking.interno;

/** Lo que envía la app del repartidor cada 5 s. {@code pedidoId} es nulo si no lleva entrega. */
public record ReporteUbicacion(Long repartidorId, Long pedidoId, Double lat, Double lng) {
}
