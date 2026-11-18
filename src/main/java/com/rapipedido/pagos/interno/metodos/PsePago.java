package com.rapipedido.pagos.interno.metodos;

import com.rapipedido.pagos.interno.MetodoPago;
import com.rapipedido.pagos.interno.PasarelaPagosSimulada;
import com.rapipedido.pagos.interno.ResultadoCobro;
import com.rapipedido.pagos.interno.SolicitudCobro;
import org.springframework.stereotype.Component;

@Component
public class PsePago implements MetodoPago {

    private final PasarelaPagosSimulada pasarela;

    public PsePago(PasarelaPagosSimulada pasarela) {
        this.pasarela = pasarela;
    }

    @Override
    public String codigo() {
        return "PSE";
    }

    @Override
    public String descripcion() {
        return "Débito bancario PSE";
    }

    @Override
    public ResultadoCobro cobrar(SolicitudCobro solicitud) {
        return pasarela.cobrar(solicitud, "PSE");
    }
}
