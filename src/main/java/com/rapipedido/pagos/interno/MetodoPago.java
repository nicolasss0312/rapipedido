package com.rapipedido.pagos.interno;

/**
 * Punto de extensión de pagos (táctica de bajo acoplamiento).
 *
 * <p>Cada implementación anotada con {@code @Component} se registra sola en
 * {@link RegistroMetodosPago}; el resto del sistema solo ve su {@link #codigo()}.
 */
public interface MetodoPago {

    /** Código que envía el cliente, por ejemplo {@code TARJETA}. */
    String codigo();

    String descripcion();

    /**
     * @throws PasarelaNoDisponibleException si el proveedor no responde; el cobro se reintentará
     */
    ResultadoCobro cobrar(SolicitudCobro solicitud);
}
