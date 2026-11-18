package com.rapipedido.pedidos.interno;

import com.rapipedido.eventos.EntregaConfirmada;
import com.rapipedido.eventos.Eventos;
import com.rapipedido.eventos.PagoAprobado;
import com.rapipedido.eventos.PagoFallido;
import com.rapipedido.eventos.PedidoRecogido;
import com.rapipedido.eventos.RepartidorAsignado;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El pedido avanza cuando otros módulos publican sus eventos. */
@Configuration
public class PedidoEventosListener {

    static final String PAGO_APROBADO = "pedidos.pago-aprobado";
    static final String PAGO_FALLIDO = "pedidos.pago-fallido";
    static final String REPARTIDOR_ASIGNADO = "pedidos.repartidor-asignado";
    static final String PEDIDO_RECOGIDO = "pedidos.pedido-recogido";
    static final String ENTREGA_CONFIRMADA = "pedidos.entrega-confirmada";

    private final PedidoService pedidos;

    public PedidoEventosListener(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @Bean
    public Declarables colasPedidos() {
        return Eventos.suscripciones(
                Eventos.suscripcion(PAGO_APROBADO, PagoAprobado.class),
                Eventos.suscripcion(PAGO_FALLIDO, PagoFallido.class),
                Eventos.suscripcion(REPARTIDOR_ASIGNADO, RepartidorAsignado.class),
                Eventos.suscripcion(PEDIDO_RECOGIDO, PedidoRecogido.class),
                Eventos.suscripcion(ENTREGA_CONFIRMADA, EntregaConfirmada.class));
    }

    @RabbitListener(queues = PAGO_APROBADO)
    public void pagoAprobado(PagoAprobado evento) {
        pedidos.aplicarCambioExterno(evento.pedidoId(), EstadoPedido.PAGADO, null);
    }

    @RabbitListener(queues = PAGO_FALLIDO)
    public void pagoFallido(PagoFallido evento) {
        pedidos.aplicarCambioExterno(evento.pedidoId(), EstadoPedido.PAGO_FALLIDO, null);
    }

    @RabbitListener(queues = REPARTIDOR_ASIGNADO)
    public void repartidorAsignado(RepartidorAsignado evento) {
        pedidos.aplicarCambioExterno(evento.pedidoId(), EstadoPedido.ASIGNADO, evento.repartidorId());
    }

    @RabbitListener(queues = PEDIDO_RECOGIDO)
    public void pedidoRecogido(PedidoRecogido evento) {
        pedidos.aplicarCambioExterno(evento.pedidoId(), EstadoPedido.EN_CAMINO, null);
    }

    @RabbitListener(queues = ENTREGA_CONFIRMADA)
    public void entregaConfirmada(EntregaConfirmada evento) {
        pedidos.aplicarCambioExterno(evento.pedidoId(), EstadoPedido.ENTREGADO, null);
    }
}
