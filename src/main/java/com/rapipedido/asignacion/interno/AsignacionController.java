package com.rapipedido.asignacion.interno;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints que usa la app del repartidor. */
@RestController
@RequestMapping("/api")
public class AsignacionController {

    public record RepartidorResponse(Long id, String nombre, String telefono, EstadoRepartidor estado) {
        static RepartidorResponse de(Repartidor r) {
            return new RepartidorResponse(r.getId(), r.getNombre(), r.getTelefono(), r.getEstado());
        }
    }

    public record EntregaResponse(Long pedidoId, Long restauranteId, Long repartidorId, EstadoAsignacion estado,
                                  double latRecogida, double lngRecogida) {
        static EntregaResponse de(Asignacion a) {
            return new EntregaResponse(a.getPedidoId(), a.getRestauranteId(), a.getRepartidorId(), a.getEstado(),
                    a.getLat(), a.getLng());
        }
    }

    public record CambioDisponibilidad(boolean disponible) {
    }

    private final AsignacionService servicio;
    private final RepartidorRepository repartidores;

    public AsignacionController(AsignacionService servicio, RepartidorRepository repartidores) {
        this.servicio = servicio;
        this.repartidores = repartidores;
    }

    @GetMapping("/repartidores")
    public List<RepartidorResponse> repartidores() {
        return repartidores.findAll(Sort.by("id")).stream().map(RepartidorResponse::de).toList();
    }

    @PostMapping("/repartidores/{id}/disponibilidad")
    public RepartidorResponse disponibilidad(@PathVariable Long id, @RequestBody CambioDisponibilidad cambio) {
        return RepartidorResponse.de(servicio.cambiarDisponibilidad(id, cambio.disponible()));
    }

    @GetMapping("/repartidores/{id}/entrega-actual")
    public ResponseEntity<EntregaResponse> entregaActual(@PathVariable Long id) {
        return ResponseEntity.of(servicio.entregaActual(id).map(EntregaResponse::de));
    }

    @PostMapping("/entregas/{pedidoId}/recoger")
    public EntregaResponse recoger(@PathVariable Long pedidoId) {
        return EntregaResponse.de(servicio.recoger(pedidoId));
    }

    @PostMapping("/entregas/{pedidoId}/confirmar")
    public EntregaResponse confirmar(@PathVariable Long pedidoId) {
        return EntregaResponse.de(servicio.confirmarEntrega(pedidoId));
    }
}
