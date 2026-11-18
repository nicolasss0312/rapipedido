package com.rapipedido.eventos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapipedido.eventos.interno.OutboxEvento;
import com.rapipedido.eventos.interno.OutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica eventos de dominio usando el patrón <b>Transactional Outbox</b>.
 *
 * <p>El evento se inserta en la tabla {@code outbox_eventos} dentro de la misma transacción que el
 * cambio de negocio (por eso exige una transacción activa). Si la transacción se confirma, el
 * evento llegará a RabbitMQ aunque el broker esté caído en ese momento; si se revierte, el evento
 * nunca existió. Así no hay pedidos registrados sin su evento ni eventos de pedidos inexistentes.
 */
@Component
public class PublicadorEventos {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public PublicadorEventos(OutboxRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(Object evento) {
        try {
            outbox.save(new OutboxEvento(evento.getClass().getSimpleName(), mapper.writeValueAsString(evento)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar el evento " + evento, e);
        }
    }
}
