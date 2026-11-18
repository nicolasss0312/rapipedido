package com.rapipedido.catalogo;

import java.util.List;

/** Consultas del catálogo que pueden usar otros módulos. Todas las respuestas están en caché. */
public interface CatalogoApi {

    List<RestauranteInfo> restaurantes();

    /** @throws org.springframework.web.server.ResponseStatusException 404 si no existe */
    RestauranteInfo restaurante(Long id);

    /** @throws org.springframework.web.server.ResponseStatusException 404 si el restaurante no existe */
    List<ProductoInfo> menu(Long restauranteId);
}
