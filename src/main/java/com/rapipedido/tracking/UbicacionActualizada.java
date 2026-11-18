package com.rapipedido.tracking;

import java.time.Instant;

public record UbicacionActualizada(Long repartidorId, Long pedidoId, double lat, double lng, Instant momento) {
}
