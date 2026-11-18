package com.rapipedido.eventos;

import java.math.BigDecimal;

/** Un cliente confirmó un pedido; el módulo de pagos debe cobrarlo. */
public record PedidoCreado(Long pedidoId, Long clienteId, Long restauranteId, BigDecimal total, String metodoPago) {
}
