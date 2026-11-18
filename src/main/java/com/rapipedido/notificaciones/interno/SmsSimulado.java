package com.rapipedido.notificaciones.interno;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsSimulado implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(SmsSimulado.class);

    @Override
    public void enviar(String destinatario, String mensaje) {
        log.info("[SMS  → {}] {}", destinatario, mensaje);
    }
}
