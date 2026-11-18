package com.rapipedido.catalogo.interno;

import java.math.BigDecimal;
import java.util.List;

import com.rapipedido.catalogo.ProductoInfo;
import com.rapipedido.catalogo.RestauranteInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogoController {

    public record NuevoProducto(@NotBlank String nombre, String descripcion, @NotNull @Positive BigDecimal precio) {
    }

    public record CambioProducto(@Positive BigDecimal precio, Boolean disponible) {
    }

    public record CambioApertura(boolean abierto) {
    }

    private final CatalogoServicio catalogo;

    public CatalogoController(CatalogoServicio catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/restaurantes")
    public List<RestauranteInfo> restaurantes() {
        return catalogo.restaurantes();
    }

    @GetMapping("/restaurantes/{id}")
    public RestauranteInfo restaurante(@PathVariable Long id) {
        return catalogo.restaurante(id);
    }

    @GetMapping("/restaurantes/{id}/menu")
    public List<ProductoInfo> menu(@PathVariable Long id) {
        return catalogo.menu(id);
    }

    /** Panel del restaurante: agregar un producto al menú. */
    @PostMapping("/restaurantes/{id}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoInfo agregarProducto(@PathVariable Long id, @Valid @RequestBody NuevoProducto producto) {
        return catalogo.agregarProducto(id, producto.nombre(), producto.descripcion(), producto.precio());
    }

    /** Panel del restaurante: cambiar precio o marcar como agotado. */
    @PatchMapping("/productos/{id}")
    public ProductoInfo actualizarProducto(@PathVariable Long id, @Valid @RequestBody CambioProducto cambio) {
        return catalogo.actualizarProducto(id, cambio.precio(), cambio.disponible());
    }

    @PatchMapping("/restaurantes/{id}/apertura")
    public RestauranteInfo cambiarApertura(@PathVariable Long id, @RequestBody CambioApertura cambio) {
        return catalogo.cambiarApertura(id, cambio.abierto());
    }
}
