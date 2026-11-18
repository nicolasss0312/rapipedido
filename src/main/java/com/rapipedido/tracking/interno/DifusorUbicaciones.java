package com.rapipedido.tracking.interno;

import com.rapipedido.tracking.UbicacionActualizada;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Recibe las posiciones desde RabbitMQ y las empuja por WebSocket a los clientes de esta réplica. */
@Component
public class DifusorUbicaciones {

    private final SimpMessagingTemplate websocket;

    public DifusorUbicaciones(SimpMessagingTemplate websocket) {
        this.websocket = websocket;
    }

    @RabbitListener(queues = "#{colaTrackingInstancia.name}")
    public void difundir(UbicacionActualizada ubicacion) {
        if (ubicacion.pedidoId() != null) {
            websocket.convertAndSend("/topic/pedidos/" + ubicacion.pedidoId() + "/ubicacion", ubicacion);
        }
    }
}
