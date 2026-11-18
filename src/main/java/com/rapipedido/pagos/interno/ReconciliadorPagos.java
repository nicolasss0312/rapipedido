package com.rapipedido.pagos.interno;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Segunda línea de defensa: retoma los pagos que siguen PENDIENTES después de que RabbitMQ agotó sus
 * reintentos (por ejemplo, si la pasarela estuvo caída varios minutos).
 */
@Component
public class ReconciliadorPagos {

    private static final Logger log = LoggerFactory.getLogger(ReconciliadorPagos.class);

    private final PagoRepository pagos;
    private final PagoService servicio;
    private final long reintentoTrasSegundos;

    public ReconciliadorPagos(PagoRepository pagos, PagoService servicio,
                              @Value("${rapipedido.pagos.reintento-tras-segundos:30}") long reintentoTrasSegundos) {
        this.pagos = pagos;
        this.servicio = servicio;
        this.reintentoTrasSegundos = reintentoTrasSegundos;
    }

    @Scheduled(fixedDelayString = "${rapipedido.pagos.reconciliacion-ms:15000}",
               initialDelayString = "${rapipedido.pagos.reconciliacion-ms:15000}")
    public void reintentarPendientes() {
        Instant limite = Instant.now().minusSeconds(reintentoTrasSegundos);
        for (Pago pago : pagos.findTop50ByEstadoAndActualizadoEnBeforeOrderByActualizadoEnAsc(EstadoPago.PENDIENTE, limite)) {
            try {
                servicio.intentarCobro(pago.getId());
            } catch (PasarelaNoDisponibleException e) {
                log.info("Reconciliación: el pago {} sigue pendiente", pago.getId());
            } catch (RuntimeException e) {
                log.warn("Reconciliación: error con el pago {}: {}", pago.getId(), e.getMessage());
            }
        }
    }
}
