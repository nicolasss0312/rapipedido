/**
 * Módulo de asignación de repartidores.
 *
 * <p>Escucha {@code PedidoListo}, elige el repartidor disponible más cercano al restaurante (índice
 * GEO de Redis vía {@link com.rapipedido.tracking.UbicacionRepartidores}) y publica
 * {@code RepartidorAsignado}. Si no hay nadie libre, la asignación queda PENDIENTE y se reintenta.
 * También recibe las confirmaciones de recogida y entrega del repartidor.
 */
package com.rapipedido.asignacion;
