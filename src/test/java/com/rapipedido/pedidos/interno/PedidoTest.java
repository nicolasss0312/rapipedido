package com.rapipedido.pedidos.interno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PedidoTest {

    private Pedido nuevoPedido() {
        Pedido pedido = new Pedido(1L, 1L, "TARJETA", "Calle 1", null);
        pedido.agregarItem(10L, "Arepa", new BigDecimal("12000"), 2);
        pedido.liquidar(new BigDecimal("5000"), new BigDecimal("3600"));
        return pedido;
    }

    @Test
    void calculaElTotalConEnvio() {
        Pedido pedido = nuevoPedido();
        assertThat(pedido.getSubtotal()).isEqualByComparingTo("24000");
        assertThat(pedido.getTotal()).isEqualByComparingTo("29000");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PAGO_PENDIENTE);
    }

    @Test
    void recorreElFlujoCompleto() {
        Pedido pedido = nuevoPedido();
        for (EstadoPedido siguiente : new EstadoPedido[] {EstadoPedido.PAGADO, EstadoPedido.ACEPTADO,
                EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO, EstadoPedido.ASIGNADO, EstadoPedido.EN_CAMINO,
                EstadoPedido.ENTREGADO}) {
            pedido.cambiarEstado(siguiente);
        }
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void noPermiteSaltarseElPago() {
        Pedido pedido = nuevoPedido();
        assertThatThrownBy(() -> pedido.cambiarEstado(EstadoPedido.ACEPTADO))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void noPermiteCancelarUnPedidoEnPreparacion() {
        Pedido pedido = nuevoPedido();
        pedido.cambiarEstado(EstadoPedido.PAGADO);
        pedido.cambiarEstado(EstadoPedido.ACEPTADO);
        pedido.cambiarEstado(EstadoPedido.EN_PREPARACION);
        assertThatThrownBy(() -> pedido.cambiarEstado(EstadoPedido.CANCELADO))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void comisionPorcentual() {
        assertThat(new ComisionPorcentual(new BigDecimal("0.15")).calcular(new BigDecimal("24000"), 1L))
                .isEqualByComparingTo("3600");
    }
}
