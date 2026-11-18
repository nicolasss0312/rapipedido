package com.rapipedido.asignacion.interno;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rapipedido.eventos.EntregaConfirmada;
import com.rapipedido.eventos.PedidoListo;
import com.rapipedido.eventos.PedidoRecogido;
import com.rapipedido.eventos.PublicadorEventos;
import com.rapipedido.eventos.RepartidorAsignado;
import com.rapipedido.tracking.UbicacionRepartidores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AsignacionService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionService.class);

    private final AsignacionRepository asignaciones;
    private final RepartidorRepository repartidores;
    private final UbicacionRepartidores ubicaciones;
    private final PublicadorEventos eventos;
    private final double radioKm;

    public AsignacionService(AsignacionRepository asignaciones, RepartidorRepository repartidores,
                             UbicacionRepartidores ubicaciones, PublicadorEventos eventos,
                             @Value("${rapipedido.asignacion.radio-km:5}") double radioKm) {
        this.asignaciones = asignaciones;
        this.repartidores = repartidores;
        this.ubicaciones = ubicaciones;
        this.eventos = eventos;
        this.radioKm = radioKm;
    }

    @Transactional
    public void registrar(PedidoListo evento) {
        if (asignaciones.findByPedidoId(evento.pedidoId()).isPresent()) {
            return; // evento duplicado
        }
        Asignacion asignacion = asignaciones.save(
                new Asignacion(evento.pedidoId(), evento.restauranteId(), evento.lat(), evento.lng()));
        intentarAsignar(asignacion);
    }

    @Transactional
    public void reintentar(Long asignacionId) {
        asignaciones.findById(asignacionId)
                .filter(a -> a.getEstado() == EstadoAsignacion.PENDIENTE)
                .ifPresent(this::intentarAsignar);
    }

    @Transactional
    public Asignacion recoger(Long pedidoId) {
        Asignacion asignacion = buscarPorPedido(pedidoId);
        if (asignacion.getEstado() == EstadoAsignacion.RECOGIDA) {
            return asignacion;
        }
        exigirEstado(asignacion, EstadoAsignacion.ASIGNADA);
        asignacion.recoger();
        eventos.publicar(new PedidoRecogido(pedidoId, asignacion.getRepartidorId()));
        return asignacion;
    }

    @Transactional
    public Asignacion confirmarEntrega(Long pedidoId) {
        Asignacion asignacion = buscarPorPedido(pedidoId);
        if (asignacion.getEstado() == EstadoAsignacion.ENTREGADA) {
            return asignacion;
        }
        exigirEstado(asignacion, EstadoAsignacion.RECOGIDA);
        asignacion.entregar();
        repartidores.findById(asignacion.getRepartidorId()).ifPresent(Repartidor::liberar);
        eventos.publicar(new EntregaConfirmada(pedidoId, asignacion.getRepartidorId()));
        return asignacion;
    }

    @Transactional
    public Repartidor cambiarDisponibilidad(Long repartidorId, boolean disponible) {
        Repartidor repartidor = repartidores.findById(repartidorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repartidor " + repartidorId + " no existe"));
        repartidor.cambiarConexion(disponible);
        return repartidor;
    }

    @Transactional(readOnly = true)
    public Optional<Asignacion> entregaActual(Long repartidorId) {
        return asignaciones.findFirstByRepartidorIdAndEstadoIn(repartidorId,
                EnumSet.of(EstadoAsignacion.ASIGNADA, EstadoAsignacion.RECOGIDA));
    }

    private void intentarAsignar(Asignacion asignacion) {
        Optional<Repartidor> elegido = elegirRepartidor(asignacion);
        if (elegido.isEmpty()) {
            log.info("Pedido {}: no hay repartidores disponibles, se reintentará", asignacion.getPedidoId());
            return;
        }
        Repartidor repartidor = elegido.get();
        repartidor.ocupar();
        asignacion.asignar(repartidor.getId());
        eventos.publicar(new RepartidorAsignado(asignacion.getPedidoId(), repartidor.getId()));
        log.info("Pedido {} asignado al repartidor {}", asignacion.getPedidoId(), repartidor.getId());
    }

    /** Primero el disponible más cercano al restaurante; si nadie reportó ubicación, cualquiera libre. */
    private Optional<Repartidor> elegirRepartidor(Asignacion asignacion) {
        List<Long> cercanos = ubicaciones.cercanos(asignacion.getLat(), asignacion.getLng(), radioKm);
        if (!cercanos.isEmpty()) {
            Map<Long, Repartidor> candidatos = repartidores.findAllById(cercanos).stream()
                    .filter(Repartidor::estaDisponible)
                    .collect(Collectors.toMap(Repartidor::getId, Function.identity()));
            for (Long id : cercanos) {
                if (candidatos.containsKey(id)) {
                    return Optional.of(candidatos.get(id));
                }
            }
        }
        return repartidores.findFirstByEstadoOrderByIdAsc(EstadoRepartidor.DISPONIBLE);
    }

    private Asignacion buscarPorPedido(Long pedidoId) {
        return asignaciones.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El pedido " + pedidoId + " no tiene entrega"));
    }

    private static void exigirEstado(Asignacion asignacion, EstadoAsignacion esperado) {
        if (asignacion.getEstado() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La entrega está en estado " + asignacion.getEstado() + " y se esperaba " + esperado);
        }
    }
}
