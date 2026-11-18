package com.rapipedido.pagos.interno;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PagoController {

    public record MetodoPagoResponse(String codigo, String descripcion) {
    }

    public record PagoResponse(Long id, Long pedidoId, String metodo, BigDecimal monto, EstadoPago estado,
                               int intentos, String referencia, String motivo, Instant actualizadoEn) {
        static PagoResponse de(Pago p) {
            return new PagoResponse(p.getId(), p.getPedidoId(), p.getMetodo(), p.getMonto(), p.getEstado(),
                    p.getIntentos(), p.getReferenciaExterna(), p.getMotivo(), p.getActualizadoEn());
        }
    }

    private final RegistroMetodosPago metodos;
    private final PagoRepository pagos;
    private final PasarelaPagosSimulada pasarela;

    public PagoController(RegistroMetodosPago metodos, PagoRepository pagos, PasarelaPagosSimulada pasarela) {
        this.metodos = metodos;
        this.pagos = pagos;
        this.pasarela = pasarela;
    }

    @GetMapping("/pagos/metodos")
    public List<MetodoPagoResponse> metodos() {
        return metodos.todos().stream()
                .map(m -> new MetodoPagoResponse(m.codigo(), m.descripcion()))
                .sorted(Comparator.comparing(MetodoPagoResponse::codigo))
                .toList();
    }

    @GetMapping("/pagos/pedido/{pedidoId}")
    public PagoResponse pagoDePedido(@PathVariable Long pedidoId) {
        return pagos.findByPedidoId(pedidoId).map(PagoResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El pedido aún no tiene pago registrado"));
    }

    /** Administración: ver la configuración de la pasarela simulada. */
    @GetMapping("/admin/pasarela")
    public PasarelaPagosSimulada.Configuracion pasarela() {
        return pasarela.configuracion();
    }

    /** Administración: provocar caídas, fallos aleatorios o latencia para probar la disponibilidad. */
    @PutMapping("/admin/pasarela")
    public PasarelaPagosSimulada.Configuracion configurarPasarela(@RequestBody PasarelaPagosSimulada.Configuracion config) {
        return pasarela.configurar(config);
    }
}
