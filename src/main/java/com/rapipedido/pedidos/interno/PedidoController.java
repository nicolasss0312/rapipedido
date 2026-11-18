package com.rapipedido.pedidos.interno;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.rapipedido.pedidos.interno.Dtos.CambioEstadoRequest;
import com.rapipedido.pedidos.interno.Dtos.CrearPedidoRequest;
import com.rapipedido.pedidos.interno.Dtos.PedidoResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidos;

    public PedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    /**
     * Confirma el carrito. Responde <b>202 Accepted</b>: el pedido quedó registrado y el pago se
     * procesa en segundo plano. La cabecera opcional {@code Idempotency-Key} evita pedidos duplicados
     * cuando la app reintenta por mala conexión.
     */
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest solicitud,
                                                @RequestHeader(value = "Idempotency-Key", required = false) String clave) {
        PedidoResponse pedido;
        try {
            pedido = pedidos.crear(solicitud, clave);
        } catch (DataIntegrityViolationException e) {
            if (clave == null) {
                throw e;
            }
            pedido = pedidos.obtenerPorClave(clave); // dos reintentos simultáneos con la misma clave
        }
        return ResponseEntity.accepted().location(URI.create("/api/pedidos/" + pedido.id())).body(pedido);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidos.obtener(id);
    }

    /** Historial del cliente o bandeja del restaurante. */
    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) Long clienteId,
                                       @RequestParam(required = false) Long restauranteId) {
        return pedidos.listar(clienteId, restauranteId);
    }

    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest cambio) {
        return pedidos.cambiarEstadoPorRestaurante(id, cambio.estado());
    }

    /** Reporte para administración. */
    @GetMapping("/resumen")
    public Map<String, Long> resumen() {
        return pedidos.resumenPorEstado();
    }
}
