package com.rapipedido.notificaciones.interno;

/** Adaptador hacia un proveedor externo (Firebase, Twilio...). Aquí solo se simulan. */
public interface CanalNotificacion {

    void enviar(String destinatario, String mensaje);
}
