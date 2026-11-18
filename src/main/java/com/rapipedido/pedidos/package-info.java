/**
 * Módulo de pedidos: carrito, registro del pedido y su máquina de estados.
 *
 * <p>Registrar un pedido es rápido (meta: menos de 2 s en el p95): valida contra el catálogo en caché,
 * guarda el pedido y su evento {@code PedidoCreado} en una sola transacción y responde 202. El cobro
 * ocurre después, de forma asíncrona, en el módulo de pagos.
 *
 * <p>No expone API a otros módulos; reacciona a los eventos de pagos y asignación.
 */
package com.rapipedido.pedidos;
