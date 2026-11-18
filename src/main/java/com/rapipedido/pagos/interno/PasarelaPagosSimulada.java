package com.rapipedido.pagos.interno;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Simula la pasarela de pagos externa.
 *
 * <p>Su configuración (caída, tasa de fallos, latencia) vive en Redis para que todas las réplicas del
 * monolito vean la misma "pasarela" y se pueda provocar una caída en vivo con
 * {@code PUT /api/admin/pasarela}. También guarda los cobros por clave de idempotencia, como lo haría
 * una pasarela real: repetir la misma solicitud devuelve el mismo resultado sin cobrar dos veces.
 */
@Component
public class PasarelaPagosSimulada {

    public record Configuracion(boolean caida, double tasaFallo, double tasaRechazo, long latenciaMs) {
    }

    private static final String CLAVE_CONFIG = "pasarela:simulada:config";
    private static final String PREFIJO_COBRO = "pasarela:simulada:cobro:";

    private final StringRedisTemplate redis;
    private final Configuracion porDefecto;
    private final long timeoutMs;

    public PasarelaPagosSimulada(StringRedisTemplate redis,
                                 @Value("${rapipedido.pasarela.tasa-fallo:0}") double tasaFallo,
                                 @Value("${rapipedido.pasarela.tasa-rechazo:0}") double tasaRechazo,
                                 @Value("${rapipedido.pasarela.latencia-ms:300}") long latenciaMs,
                                 @Value("${rapipedido.pasarela.timeout-ms:3000}") long timeoutMs) {
        this.redis = redis;
        this.porDefecto = new Configuracion(false, tasaFallo, tasaRechazo, latenciaMs);
        this.timeoutMs = timeoutMs;
    }

    public ResultadoCobro cobrar(SolicitudCobro solicitud, String medio) {
        String claveCobro = PREFIJO_COBRO + solicitud.claveIdempotencia();
        String previo = redis.opsForValue().get(claveCobro);
        if (previo != null) {
            return deserializar(previo);
        }

        Configuracion config = configuracion();
        dormir(Math.min(config.latenciaMs(), timeoutMs));
        if (config.latenciaMs() >= timeoutMs) {
            throw new PasarelaNoDisponibleException("Timeout: la pasarela no respondió en " + timeoutMs + " ms");
        }
        ThreadLocalRandom azar = ThreadLocalRandom.current();
        if (config.caida() || azar.nextDouble() < config.tasaFallo()) {
            throw new PasarelaNoDisponibleException("La pasarela respondió HTTP 503");
        }

        ResultadoCobro resultado = azar.nextDouble() < config.tasaRechazo()
                ? ResultadoCobro.rechazado("Transacción rechazada por el banco")
                : ResultadoCobro.aprobado(medio + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        redis.opsForValue().set(claveCobro, serializar(resultado), Duration.ofDays(1));
        return resultado;
    }

    public Configuracion configuracion() {
        Map<Object, Object> valores = redis.opsForHash().entries(CLAVE_CONFIG);
        if (valores.isEmpty()) {
            return porDefecto;
        }
        return new Configuracion(
                Boolean.parseBoolean((String) valores.get("caida")),
                Double.parseDouble((String) valores.get("tasaFallo")),
                Double.parseDouble((String) valores.get("tasaRechazo")),
                Long.parseLong((String) valores.get("latenciaMs")));
    }

    public Configuracion configurar(Configuracion nueva) {
        redis.opsForHash().putAll(CLAVE_CONFIG, Map.of(
                "caida", String.valueOf(nueva.caida()),
                "tasaFallo", String.valueOf(nueva.tasaFallo()),
                "tasaRechazo", String.valueOf(nueva.tasaRechazo()),
                "latenciaMs", String.valueOf(nueva.latenciaMs())));
        return nueva;
    }

    private static String serializar(ResultadoCobro r) {
        return (r.aprobado() ? "APROBADO|" + r.referencia() : "RECHAZADO|" + r.motivo());
    }

    private static ResultadoCobro deserializar(String valor) {
        String[] partes = valor.split("\\|", 2);
        return "APROBADO".equals(partes[0]) ? ResultadoCobro.aprobado(partes[1]) : ResultadoCobro.rechazado(partes[1]);
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PasarelaNoDisponibleException("Cobro interrumpido");
        }
    }
}
