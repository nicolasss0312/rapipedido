package com.rapipedido.pedidos.interno;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Objetos de entrada y salida de la API de pedidos. */
public final class Dtos {

    private Dtos() {
    }

    public record CrearPedidoRequest(@NotNull Long clienteId,
                                     @NotNull Long restauranteId,
                                     @NotBlank String metodoPago,
                                     @NotBlank String direccionEntrega,
                                     @NotEmpty List<@Valid ItemRequest> items) {
    }

    public record ItemRequest(@NotNull Long productoId, @Min(1) int cantidad) {
    }

    public record CambioEstadoRequest(@NotNull EstadoPedido estado) {
    }

    public record ItemResponse(Long productoId, String nombre, BigDecimal precioUnitario, int cantidad,
                               BigDecimal subtotal) {
    }

    public record PedidoResponse(Long id, Long clienteId, Long restauranteId, EstadoPedido estado,
                                 String metodoPago, String direccionEntrega, BigDecimal subtotal,
                                 BigDecimal costoEnvio, BigDecimal comision, BigDecimal total, Long repartidorId,
                                 Instant creadoEn, Instant actualizadoEn, List<ItemResponse> items) {

        static PedidoResponse de(Pedido p) {
            return new PedidoResponse(p.getId(), p.getClienteId(), p.getRestauranteId(), p.getEstado(),
                    p.getMetodoPago(), p.getDireccionEntrega(), p.getSubtotal(), p.getCostoEnvio(), p.getComision(),
                    p.getTotal(), p.getRepartidorId(), p.getCreadoEn(), p.getActualizadoEn(),
                    p.getItems().stream()
                            .map(i -> new ItemResponse(i.getProductoId(), i.getNombre(), i.getPrecioUnitario(),
                                    i.getCantidad(), i.getSubtotal()))
                            .toList());
        }
    }
}
