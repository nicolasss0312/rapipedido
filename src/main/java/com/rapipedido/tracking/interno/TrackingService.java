package com.rapipedido.tracking.interno;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapipedido.tracking.UbicacionActualizada;
import com.rapipedido.tracking.UbicacionRepartidores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

/**
 * Las posiciones solo viven en Redis: llegan miles por minuto y no necesitan historial en PostgreSQL.
 * Tampoco pasan por el outbox; perder una posición no importa porque la siguiente llega en 5 s.
 */
@Service
public class TrackingService implements UbicacionRepartidores {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
    static final String CLAVE_GEO = "tracking:repartidores";
    static final String PREFIJO_PEDIDO = "tracking:pedido:";

    private final StringRedisTemplate redis;
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;

    public TrackingService(StringRedisTemplate redis, RabbitTemplate rabbit, ObjectMapper mapper) {
        this.redis = redis;
        this.rabbit = rabbit;
        this.mapper = mapper;
    }

    public UbicacionActualizada registrar(ReporteUbicacion reporte) {
        if (reporte.repartidorId() == null || reporte.lat() == null || reporte.lng() == null) {
            throw new IllegalArgumentException("repartidorId, lat y lng son obligatorios");
        }
        UbicacionActualizada ubicacion = new UbicacionActualizada(reporte.repartidorId(), reporte.pedidoId(),
                reporte.lat(), reporte.lng(), Instant.now());

        redis.opsForGeo().add(CLAVE_GEO, new Point(reporte.lng(), reporte.lat()), reporte.repartidorId().toString());
        if (reporte.pedidoId() != null) {
            redis.opsForValue().set(PREFIJO_PEDIDO + reporte.pedidoId(), aJson(ubicacion), Duration.ofHours(2));
        }
        rabbit.convertAndSend(TrackingConfig.EXCHANGE, "", ubicacion);
        return ubicacion;
    }

    public Optional<UbicacionActualizada> ultimaUbicacion(Long pedidoId) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIJO_PEDIDO + pedidoId)).map(this::deJson);
    }

    @Override
    public List<Long> cercanos(double lat, double lng, double radioKm) {
        try {
            GeoResults<RedisGeoCommands.GeoLocation<String>> resultados = redis.opsForGeo().search(CLAVE_GEO,
                    GeoReference.fromCoordinate(new Point(lng, lat)),
                    new Distance(radioKm, Metrics.KILOMETERS),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().sortAscending().limit(20));
            if (resultados == null) {
                return List.of();
            }
            return resultados.getContent().stream().map(r -> Long.valueOf(r.getContent().getName())).toList();
        } catch (DataAccessException e) {
            // Degradación: sin Redis se asigna sin criterio de cercanía en lugar de fallar.
            log.warn("No se pudo consultar ubicaciones en Redis: {}", e.getMessage());
            return List.of();
        }
    }

    private String aJson(UbicacionActualizada u) {
        try {
            return mapper.writeValueAsString(u);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private UbicacionActualizada deJson(String json) {
        try {
            return mapper.readValue(json, UbicacionActualizada.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
