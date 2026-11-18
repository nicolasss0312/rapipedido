package com.rapipedido.tracking.interno;

import com.rapipedido.tracking.UbicacionActualizada;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService tracking;

    public TrackingController(TrackingService tracking) {
        this.tracking = tracking;
    }

    /** Canal principal: la app del repartidor envía por STOMP a {@code /app/ubicacion}. */
    @MessageMapping("/ubicacion")
    public void recibirPorWebSocket(ReporteUbicacion reporte) {
        tracking.registrar(reporte);
    }

    /** Alternativa HTTP para pruebas con curl. */
    @PostMapping("/ubicacion")
    public UbicacionActualizada recibirPorHttp(@RequestBody ReporteUbicacion reporte) {
        return tracking.registrar(reporte);
    }

    /** Última posición conocida (útil cuando el cliente abre la app y aún no llega un evento). */
    @GetMapping("/pedidos/{pedidoId}/ubicacion")
    public ResponseEntity<UbicacionActualizada> ultima(@PathVariable Long pedidoId) {
        return ResponseEntity.of(tracking.ultimaUbicacion(pedidoId));
    }
}
