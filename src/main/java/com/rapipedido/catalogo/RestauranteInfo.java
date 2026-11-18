package com.rapipedido.catalogo;

import java.io.Serializable;

public record RestauranteInfo(Long id, String nombre, String direccion, double lat, double lng, boolean abierto)
        implements Serializable {
}
