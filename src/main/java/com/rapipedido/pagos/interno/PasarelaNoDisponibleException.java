package com.rapipedido.pagos.interno;

/** Falla técnica de la pasarela (caída o timeout). A diferencia de un rechazo, se reintenta. */
public class PasarelaNoDisponibleException extends RuntimeException {

    public PasarelaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
