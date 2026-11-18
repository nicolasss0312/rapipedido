package com.rapipedido.notificaciones.interno;

import com.rapipedido.eventos.EstadoPedidoCambiado;
import com.rapipedido.eventos.Eventos;
import com.rapipedido.eventos.RepartidorAsignado;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificacionesListener {

    static final String COLA_ESTADO = "notificaciones.estado-pedido";
    static final String COLA_ASIGNACION = "notificaciones.repartidor-asignado";

    private final PushSimulado push;
    private final SmsSimulado sms;

    public NotificacionesListener(PushSimulado push, SmsSimulado sms) {
        this.push = push;
        this.sms = sms;
    }

    @Bean
    public Declarables colasNotificaciones() {
        return Eventos.suscripciones(
                Eventos.suscripcion(COLA_ESTADO, EstadoPedidoCambiado.class),
                Eventos.suscripcion(COLA_ASIGNACION, RepartidorAsignado.class));
    }

    @RabbitListener(queues = COLA_ESTADO)
    public void alCambiarEstado(EstadoPedidoCambiado e) {
        String cliente = "cliente-" + e.clienteId();
        String restaurante = "restaurante-" + e.restauranteId();
        String pedido = "#" + e.pedidoId();
        switch (e.estadoNuevo()) {
            case "PAGADO" -> {
                push.enviar(restaurante, "Nuevo pedido " + pedido + " pagado. ¡A cocinar!");
                push.enviar(cliente, "Pago confirmado. El restaurante recibió tu pedido " + pedido);
            }
            case "PAGO_FALLIDO" -> {
                push.enviar(cliente, "No pudimos cobrar tu pedido " + pedido + ". Intenta con otro método de pago.");
                sms.enviar(cliente, "RapiPedido: el pago del pedido " + pedido + " no se pudo completar.");
            }
            case "ACEPTADO" -> push.enviar(cliente, "El restaurante aceptó tu pedido " + pedido);
            case "EN_PREPARACION" -> push.enviar(cliente, "Tu pedido " + pedido + " se está preparando");
            case "LISTO" -> push.enviar(cliente, "Tu pedido " + pedido + " está listo; buscando repartidor");
            case "ASIGNADO" -> push.enviar(cliente, "Un repartidor va camino al restaurante por tu pedido " + pedido);
            case "EN_CAMINO" -> {
                push.enviar(cliente, "Tu pedido " + pedido + " va en camino. Síguelo en vivo en la app.");
                sms.enviar(cliente, "RapiPedido: tu pedido " + pedido + " va en camino.");
            }
            case "ENTREGADO" -> push.enviar(cliente, "Pedido " + pedido + " entregado. ¡Buen provecho!");
            case "CANCELADO" -> push.enviar(cliente, "Tu pedido " + pedido + " fue cancelado");
            default -> {
            }
        }
    }

    @RabbitListener(queues = COLA_ASIGNACION)
    public void alAsignarRepartidor(RepartidorAsignado e) {
        push.enviar("repartidor-" + e.repartidorId(), "Nueva entrega: recoge el pedido #" + e.pedidoId());
    }
}
