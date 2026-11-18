package com.rapipedido.pagos.interno.metodos;

import com.rapipedido.pagos.interno.MetodoPago;
import com.rapipedido.pagos.interno.PasarelaPagosSimulada;
import com.rapipedido.pagos.interno.ResultadoCobro;
import com.rapipedido.pagos.interno.SolicitudCobro;
import org.springframework.stereotype.Component;

@Component
public class TarjetaCreditoPago implements MetodoPago {

    private final PasarelaPagosSimulada pasarela;

    public TarjetaCreditoPago(PasarelaPagosSimulada pasarela) {
        this.pasarela = pasarela;
    }

    @Override
    public String codigo() {
        return "TARJETA";
    }

    @Override
    public String descripcion() {
        return "Tarjeta de crédito o débito";
    }

    @Override
    public ResultadoCobro cobrar(SolicitudCobro solicitud) {
        return pasarela.cobrar(solicitud, "TC");
    }
}
