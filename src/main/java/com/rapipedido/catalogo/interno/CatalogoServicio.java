package com.rapipedido.catalogo.interno;

import java.math.BigDecimal;
import java.util.List;

import com.rapipedido.catalogo.CatalogoApi;
import com.rapipedido.catalogo.ProductoInfo;
import com.rapipedido.catalogo.RestauranteInfo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogoServicio implements CatalogoApi {

    private final RestauranteRepository restaurantes;
    private final ProductoRepository productos;

    public CatalogoServicio(RestauranteRepository restaurantes, ProductoRepository productos) {
        this.restaurantes = restaurantes;
        this.productos = productos;
    }

    @Override
    @Cacheable("restaurantes")
    @Transactional(readOnly = true)
    public List<RestauranteInfo> restaurantes() {
        return restaurantes.findAll(Sort.by("id")).stream().map(Restaurante::aInfo).toList();
    }

    @Override
    @Cacheable(value = "restaurante", key = "#id")
    @Transactional(readOnly = true)
    public RestauranteInfo restaurante(Long id) {
        return buscarRestaurante(id).aInfo();
    }

    @Override
    @Cacheable(value = "menu", key = "#restauranteId")
    @Transactional(readOnly = true)
    public List<ProductoInfo> menu(Long restauranteId) {
        buscarRestaurante(restauranteId);
        return productos.findByRestauranteIdOrderById(restauranteId).stream().map(Producto::aInfo).toList();
    }

    @CacheEvict(value = "menu", key = "#restauranteId")
    @Transactional
    public ProductoInfo agregarProducto(Long restauranteId, String nombre, String descripcion, BigDecimal precio) {
        buscarRestaurante(restauranteId);
        return productos.save(new Producto(restauranteId, nombre, descripcion, precio)).aInfo();
    }

    @CacheEvict(value = "menu", allEntries = true)
    @Transactional
    public ProductoInfo actualizarProducto(Long productoId, BigDecimal precio, Boolean disponible) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto " + productoId + " no existe"));
        producto.actualizar(precio, disponible);
        return producto.aInfo();
    }

    @Caching(evict = {
            @CacheEvict(value = "restaurantes", allEntries = true),
            @CacheEvict(value = "restaurante", key = "#id")
    })
    @Transactional
    public RestauranteInfo cambiarApertura(Long id, boolean abierto) {
        Restaurante restaurante = buscarRestaurante(id);
        restaurante.cambiarApertura(abierto);
        return restaurante.aInfo();
    }

    private Restaurante buscarRestaurante(Long id) {
        return restaurantes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante " + id + " no existe"));
    }
}
