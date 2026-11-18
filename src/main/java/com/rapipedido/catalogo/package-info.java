/**
 * Módulo de catálogo: restaurantes, menús y productos.
 *
 * <p>API pública: {@link com.rapipedido.catalogo.CatalogoApi}. Las lecturas se sirven desde Redis
 * (táctica de caché para el atributo de rendimiento); se invalidan cuando el restaurante cambia su menú.
 */
package com.rapipedido.catalogo;
