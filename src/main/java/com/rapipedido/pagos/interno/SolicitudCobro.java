package com.rapipedido.pagos.interno;

import java.math.BigDecimal;

/** @param claveIdempotencia identifica el cobro ante la pasarela para que un reintento no cobre dos veces */
public record SolicitudCobro(String claveIdempotencia, Long pedidoId, BigDecimal monto) {
}
