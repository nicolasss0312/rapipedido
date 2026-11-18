package com.rapipedido.eventos.interno;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.rapipedido.eventos.Eventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lleva los eventos del outbox a RabbitMQ. Solo marca un evento como publicado cuando el broker
 * confirma que lo recibió; si RabbitMQ no está disponible, los eventos esperan en la base de datos.
 * La entrega es "al menos una vez", por eso todos los consumidores son idempotentes.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int TAMANO_LOTE = 100;

    private final OutboxRepository outbox;
    private final RabbitTemplate rabbit;

    public OutboxRelay(OutboxRepository outbox, RabbitTemplate rabbit) {
        this.outbox = outbox;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${rapipedido.outbox.intervalo-ms:200}")
    @Transactional
    public void publicarPendientes() {
        List<OutboxEvento> pendientes = outbox.bloquearPendientes(TAMANO_LOTE);
        if (pendientes.isEmpty()) {
            return;
        }
        try {
            rabbit.invoke(operaciones -> {
                for (OutboxEvento evento : pendientes) {
                    operaciones.send(Eventos.EXCHANGE, evento.getTipo(), aMensaje(evento));
                }
                operaciones.waitForConfirmsOrDie(5_000);
                return null;
            });
        } catch (AmqpException e) {
            log.warn("RabbitMQ no disponible; {} eventos siguen en el outbox: {}", pendientes.size(), e.getMessage());
            return;
        }
        Instant ahora = Instant.now();
        pendientes.forEach(e -> e.marcarPublicado(ahora));
        log.debug("Publicados {} eventos", pendientes.size());
    }

    /** Limpieza: los eventos ya publicados se conservan un día para auditoría. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgarPublicados() {
        int borrados = outbox.borrarPublicadosAntesDe(Instant.now().minus(Duration.ofDays(1)));
        if (borrados > 0) {
            log.info("Outbox: {} eventos antiguos eliminados", borrados);
        }
    }

    private static Message aMensaje(OutboxEvento evento) {
        return MessageBuilder.withBody(evento.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(String.valueOf(evento.getId()))
                .setType(evento.getTipo())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
    }
}
