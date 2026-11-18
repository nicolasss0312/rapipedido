package com.rapipedido.asignacion.interno;

import com.rapipedido.eventos.Eventos;
import com.rapipedido.eventos.PedidoListo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class AsignacionEventosListener {

    private static final Logger log = LoggerFactory.getLogger(AsignacionEventosListener.class);
    static final String COLA_PEDIDO_LISTO = "asignacion.pedido-listo";

    private final AsignacionService servicio;
    private final AsignacionRepository asignaciones;

    public AsignacionEventosListener(AsignacionService servicio, AsignacionRepository asignaciones) {
        this.servicio = servicio;
        this.asignaciones = asignaciones;
    }

    @Bean
    public Declarables colasAsignacion() {
        return Eventos.suscripciones(Eventos.suscripcion(COLA_PEDIDO_LISTO, PedidoListo.class));
    }

    @RabbitListener(queues = COLA_PEDIDO_LISTO)
    public void alEstarListo(PedidoListo evento) {
        servicio.registrar(evento);
    }

    /** Reintenta los pedidos que quedaron sin repartidor (por ejemplo, en hora pico). */
    @Scheduled(fixedDelayString = "${rapipedido.asignacion.reintento-ms:10000}")
    public void reintentarPendientes() {
        for (Long id : asignaciones.idsPendientes()) {
            try {
                servicio.reintentar(id);
            } catch (RuntimeException e) {
                log.debug("Asignación {} no se pudo completar todavía: {}", id, e.getMessage());
            }
        }
    }
}
