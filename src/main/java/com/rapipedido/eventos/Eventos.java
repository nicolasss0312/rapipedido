package com.rapipedido.eventos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;

/**
 * Topología de RabbitMQ compartida.
 *
 * <p>Todos los eventos se publican en el exchange {@value #EXCHANGE} con el nombre simple de la
 * clase como routing key (por ejemplo {@code PedidoCreado}). Cada módulo declara sus propias colas
 * con {@link #suscripciones(Suscripcion...)}; todas tienen cola de mensajes muertos.
 */
public final class Eventos {

    public static final String EXCHANGE = "rapipedido.eventos";
    public static final String EXCHANGE_DLX = "rapipedido.eventos.dlx";
    public static final String COLA_DLQ = "rapipedido.eventos.dlq";

    private Eventos() {
    }

    /** Una cola durable de un módulo, suscrita a un tipo de evento. */
    public record Suscripcion(String cola, Class<?> tipoEvento) {
    }

    public static Suscripcion suscripcion(String cola, Class<?> tipoEvento) {
        return new Suscripcion(cola, tipoEvento);
    }

    /** Declara las colas (con dead-letter exchange) y sus enlaces al exchange de eventos. */
    public static Declarables suscripciones(Suscripcion... suscripciones) {
        TopicExchange exchange = new TopicExchange(EXCHANGE);
        List<Declarable> declarables = Arrays.stream(suscripciones)
                .flatMap(s -> {
                    Queue cola = QueueBuilder.durable(s.cola()).deadLetterExchange(EXCHANGE_DLX).build();
                    return Stream.<Declarable>of(cola,
                            BindingBuilder.bind(cola).to(exchange).with(s.tipoEvento().getSimpleName()));
                })
                .toList();
        return new Declarables(declarables);
    }
}
