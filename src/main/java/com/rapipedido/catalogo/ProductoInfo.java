package com.rapipedido.catalogo;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductoInfo(Long id, Long restauranteId, String nombre, String descripcion, BigDecimal precio,
                           boolean disponible) implements Serializable {
}
