/**
 * Módulo de eventos (kernel compartido).
 *
 * <p>Contiene los contratos de los eventos de dominio que viajan por RabbitMQ y el
 * {@link com.rapipedido.eventos.PublicadorEventos}, que los guarda en un outbox transaccional.
 * Los demás módulos solo dependen de este paquete para comunicarse entre sí, por eso no hay
 * dependencias circulares entre pedidos, pagos y asignación.
 */
package com.rapipedido.eventos;
