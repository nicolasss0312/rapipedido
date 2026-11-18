package com.rapipedido.pedidos.interno;

import java.math.BigDecimal;

/**
 * Las comisiones y promociones van a cambiar (contexto del escenario de modificabilidad). Cambiar la
 * regla es reemplazar este bean, sin tocar el flujo de pedidos.
 */
public interface PoliticaComision {

    BigDecimal calcular(BigDecimal subtotal, Long restauranteId);
}
