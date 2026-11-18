/**
 * Módulo de pagos.
 *
 * <p>Escucha {@code PedidoCreado}, cobra con la pasarela y publica {@code PagoAprobado} o
 * {@code PagoFallido}. Si la pasarela no responde, el pago queda PENDIENTE y se reintenta con espera
 * creciente (RabbitMQ) y luego con un reconciliador periódico: el pedido nunca se pierde.
 *
 * <p><b>Modificabilidad:</b> para agregar un método de pago basta con crear una clase en
 * {@code pagos.interno.metodos} que implemente {@code MetodoPago}. No se toca ningún otro módulo.
 */
package com.rapipedido.pagos;
