package com.rapipedido.pagos.interno.metodos;

import com.rapipedido.pagos.interno.MetodoPago;
import com.rapipedido.pagos.interno.ResultadoCobro;
import com.rapipedido.pagos.interno.SolicitudCobro;
import org.springframework.stereotype.Component;

/**
 * Ejemplo del escenario de modificabilidad: un método de pago nuevo es solo esta clase.
 * No pasa por la pasarela; el repartidor cobra al entregar.
 */
@Component
public class EfectivoContraEntregaPago implements MetodoPago {

    @Override
    public String codigo() {
        return "EFECTIVO";
    }

    @Override
    public String descripcion() {
        return "Efectivo contra entrega";
    }

    @Override
    public ResultadoCobro cobrar(SolicitudCobro solicitud) {
        return ResultadoCobro.aprobado("EFECTIVO-" + solicitud.pedidoId());
    }
}
