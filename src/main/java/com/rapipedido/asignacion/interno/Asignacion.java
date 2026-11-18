package com.rapipedido.asignacion.interno;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "asignaciones")
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pedidoId;

    @Column(nullable = false)
    private Long restauranteId;

    /** Ubicación del restaurante (punto de recogida). */
    private double lat;
    private double lng;

    private Long repartidorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado;

    @Column(nullable = false)
    private Instant creadaEn;

    @Column(nullable = false)
    private Instant actualizadaEn;

    @Version
    private Long version;

    protected Asignacion() {
    }

    public Asignacion(Long pedidoId, Long restauranteId, double lat, double lng) {
        this.pedidoId = pedidoId;
        this.restauranteId = restauranteId;
        this.lat = lat;
        this.lng = lng;
        this.estado = EstadoAsignacion.PENDIENTE;
        this.creadaEn = Instant.now();
        this.actualizadaEn = creadaEn;
    }

    public void asignar(Long repartidorId) {
        this.repartidorId = repartidorId;
        cambiar(EstadoAsignacion.ASIGNADA);
    }

    public void recoger() {
        cambiar(EstadoAsignacion.RECOGIDA);
    }

    public void entregar() {
        cambiar(EstadoAsignacion.ENTREGADA);
    }

    private void cambiar(EstadoAsignacion nuevo) {
        this.estado = nuevo;
        this.actualizadaEn = Instant.now();
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public Long getRestauranteId() { return restauranteId; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public Long getRepartidorId() { return repartidorId; }
    public EstadoAsignacion getEstado() { return estado; }
}
