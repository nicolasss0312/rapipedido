package com.rapipedido.pagos.interno;

import com.rapipedido.eventos.Eventos;
import com.rapipedido.eventos.PedidoCreado;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Consume {@code PedidoCreado}. Si la pasarela falla, la excepción hace que RabbitMQ reintente con
 * espera creciente (1 s, 2 s, 4 s, 8 s); al agotarse, el mensaje va a la DLQ y el
 * {@link ReconciliadorPagos} sigue intentando más tarde.
 */
@Configuration
public class PagoEventosListener {

    static final String COLA_PEDIDO_CREADO = "pagos.pedido-creado";

    private final PagoService pagos;

    public PagoEventosListener(PagoService pagos) {
        this.pagos = pagos;
    }

    @Bean
    public Declarables colasPagos() {
        return Eventos.suscripciones(Eventos.suscripcion(COLA_PEDIDO_CREADO, PedidoCreado.class));
    }

    @RabbitListener(queues = COLA_PEDIDO_CREADO)
    public void alCrearPedido(PedidoCreado evento) {
        pagos.procesar(evento);
    }
}
