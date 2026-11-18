package com.rapipedido.pagos.interno;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.rapipedido.pagos.interno.metodos.EfectivoContraEntregaPago;
import org.junit.jupiter.api.Test;

/** Escenario de modificabilidad: un método nuevo se registra sin cambiar el registro ni otros módulos. */
class RegistroMetodosPagoTest {

    /** Un método inventado en la prueba, igual al que crearía un desarrollador. */
    static class NequiPago implements MetodoPago {
        @Override public String codigo() { return "NEQUI"; }
        @Override public String descripcion() { return "Billetera Nequi"; }
        @Override public ResultadoCobro cobrar(SolicitudCobro s) { return ResultadoCobro.aprobado("NEQUI-1"); }
    }

    @Test
    void registraCualquierImplementacionDeMetodoPago() {
        RegistroMetodosPago registro = new RegistroMetodosPago(List.of(new EfectivoContraEntregaPago(), new NequiPago()));

        assertThat(registro.disponibles()).containsExactly("EFECTIVO", "NEQUI");
        assertThat(registro.soporta("nequi")).isTrue();
        assertThat(registro.soporta("BITCOIN")).isFalse();
        assertThat(registro.obtener("NEQUI").orElseThrow()
                .cobrar(new SolicitudCobro("k", 1L, java.math.BigDecimal.TEN)).aprobado()).isTrue();
    }
}
