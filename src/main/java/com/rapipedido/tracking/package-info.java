/**
 * Módulo de tracking en vivo.
 *
 * <p>El repartidor envía su posición cada 5 s por WebSocket (STOMP en {@code /ws}, destino
 * {@code /app/ubicacion}). La posición se guarda en Redis (índice GEO) y se publica en el exchange
 * fanout {@code rapipedido.tracking}; cada réplica del monolito tiene su propia cola anónima y empuja
 * la posición a los clientes conectados a ella en {@code /topic/pedidos/{id}/ubicacion}. Así el
 * tracking funciona aunque el repartidor y el cliente estén conectados a réplicas distintas.
 *
 * <p>API pública: {@link com.rapipedido.tracking.UbicacionRepartidores} (la usa asignación).
 */
package com.rapipedido.tracking;
