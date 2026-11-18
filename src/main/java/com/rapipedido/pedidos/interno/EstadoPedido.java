package com.rapipedido.pedidos.interno;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida del pedido.
 * <pre>
 * PAGO_PENDIENTE → PAGADO → ACEPTADO → EN_PREPARACION → LISTO → ASIGNADO → EN_CAMINO → ENTREGADO
 *        ↘ PAGO_FALLIDO        ↘ CANCELADO (hasta antes de la preparación)
 * </pre>
 */
public enum EstadoPedido {
    PAGO_PENDIENTE, PAGADO, PAGO_FALLIDO, ACEPTADO, EN_PREPARACION, LISTO, ASIGNADO, EN_CAMINO, ENTREGADO, CANCELADO;

    /** Estados que el restaurante puede fijar desde su panel; los demás llegan por eventos. */
    public static final Set<EstadoPedido> CONTROLADOS_POR_RESTAURANTE = EnumSet.of(ACEPTADO, EN_PREPARACION, LISTO, CANCELADO);

    private static final Map<EstadoPedido, Set<EstadoPedido>> SIGUIENTES = new EnumMap<>(EstadoPedido.class);

    static {
        SIGUIENTES.put(PAGO_PENDIENTE, EnumSet.of(PAGADO, PAGO_FALLIDO, CANCELADO));
        SIGUIENTES.put(PAGADO, EnumSet.of(ACEPTADO, CANCELADO));
        SIGUIENTES.put(ACEPTADO, EnumSet.of(EN_PREPARACION, CANCELADO));
        SIGUIENTES.put(EN_PREPARACION, EnumSet.of(LISTO));
        SIGUIENTES.put(LISTO, EnumSet.of(ASIGNADO));
        SIGUIENTES.put(ASIGNADO, EnumSet.of(EN_CAMINO));
        SIGUIENTES.put(EN_CAMINO, EnumSet.of(ENTREGADO));
        for (EstadoPedido estado : values()) {
            SIGUIENTES.putIfAbsent(estado, EnumSet.noneOf(EstadoPedido.class));
        }
    }

    public boolean puedePasarA(EstadoPedido destino) {
        return SIGUIENTES.get(this).contains(destino);
    }
}
