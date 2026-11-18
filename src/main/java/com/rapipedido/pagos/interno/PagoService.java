package com.rapipedido.pagos.interno;

import com.rapipedido.eventos.PagoAprobado;
import com.rapipedido.eventos.PagoFallido;
import com.rapipedido.eventos.PedidoCreado;
import com.rapipedido.eventos.PublicadorEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cobra los pedidos. La llamada a la pasarela se hace <b>fuera</b> de cualquier transacción para no
 * retener conexiones de la base de datos mientras se espera al proveedor externo.
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagos;
    private final RegistroMetodosPago metodos;
    private final PublicadorEventos eventos;
    private final TransactionTemplate tx;
    private final int maxIntentos;

    public PagoService(PagoRepository pagos, RegistroMetodosPago metodos, PublicadorEventos eventos,
                       TransactionTemplate tx, @Value("${rapipedido.pagos.max-intentos:10}") int maxIntentos) {
        this.pagos = pagos;
        this.metodos = metodos;
        this.eventos = eventos;
        this.tx = tx;
        this.maxIntentos = maxIntentos;
    }

    /** Idempotente: si el evento llega dos veces, se reutiliza el mismo registro de pago. */
    public void procesar(PedidoCreado pedido) {
        Pago pago = tx.execute(s -> pagos.findByPedidoId(pedido.pedidoId())
                .orElseGet(() -> pagos.save(new Pago(pedido.pedidoId(), pedido.metodoPago(), pedido.total()))));
        intentarCobro(pago.getId());
    }

    /**
     * @throws PasarelaNoDisponibleException si la pasarela falla; quien llama decide cuándo reintentar
     */
    public void intentarCobro(Long pagoId) {
        Pago pago = tx.execute(s -> {
            Pago p = pagos.findById(pagoId).orElseThrow();
            if (!p.estaPendiente()) {
                return null;
            }
            p.registrarIntento();
            return p;
        });
        if (pago == null) {
            return; // ya se resolvió (mensaje duplicado)
        }

        MetodoPago metodo = metodos.obtener(pago.getMetodo()).orElse(null);
        if (metodo == null) {
            finalizar(pagoId, ResultadoCobro.rechazado("Método de pago no soportado: " + pago.getMetodo()));
            return;
        }

        ResultadoCobro resultado;
        try {
            resultado = metodo.cobrar(new SolicitudCobro("pago-" + pago.getId(), pago.getPedidoId(), pago.getMonto()));
        } catch (PasarelaNoDisponibleException e) {
            log.warn("Pago {} (pedido {}): intento {} falló: {}", pago.getId(), pago.getPedidoId(),
                    pago.getIntentos(), e.getMessage());
            if (pago.getIntentos() >= maxIntentos) {
                finalizar(pagoId, ResultadoCobro.rechazado(
                        "La pasarela no respondió después de " + pago.getIntentos() + " intentos"));
                return;
            }
            throw e;
        }
        finalizar(pagoId, resultado);
    }

    private void finalizar(Long pagoId, ResultadoCobro resultado) {
        tx.executeWithoutResult(s -> {
            Pago p = pagos.findById(pagoId).orElseThrow();
            if (!p.estaPendiente()) {
                return;
            }
            if (resultado.aprobado()) {
                p.aprobar(resultado.referencia());
                eventos.publicar(new PagoAprobado(p.getPedidoId(), p.getId(), resultado.referencia()));
                log.info("Pago {} del pedido {} APROBADO ({})", p.getId(), p.getPedidoId(), resultado.referencia());
            } else {
                p.rechazar(resultado.motivo());
                eventos.publicar(new PagoFallido(p.getPedidoId(), p.getId(), resultado.motivo()));
                log.info("Pago {} del pedido {} RECHAZADO: {}", p.getId(), p.getPedidoId(), resultado.motivo());
            }
        });
    }
}
