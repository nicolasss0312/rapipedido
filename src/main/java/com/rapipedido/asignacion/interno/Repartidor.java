package com.rapipedido.asignacion.interno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "repartidores")
public class Repartidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRepartidor estado;

    /** Bloqueo optimista: dos réplicas no pueden asignar al mismo repartidor a la vez. */
    @Version
    private Long version;

    protected Repartidor() {
    }

    public void ocupar() {
        if (estado != EstadoRepartidor.DISPONIBLE) {
            throw new IllegalStateException("El repartidor " + id + " no está disponible");
        }
        estado = EstadoRepartidor.OCUPADO;
    }

    public void liberar() {
        estado = EstadoRepartidor.DISPONIBLE;
    }

    public void cambiarConexion(boolean disponible) {
        if (estado == EstadoRepartidor.OCUPADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El repartidor tiene una entrega en curso");
        }
        estado = disponible ? EstadoRepartidor.DISPONIBLE : EstadoRepartidor.DESCONECTADO;
    }

    public boolean estaDisponible() {
        return estado == EstadoRepartidor.DISPONIBLE;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public EstadoRepartidor getEstado() { return estado; }
}
