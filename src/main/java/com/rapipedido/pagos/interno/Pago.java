package com.rapipedido.pagos.interno;

import java.math.BigDecimal;
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
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pedidoId;

    @Column(nullable = false, length = 30)
    private String metodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    private int intentos;

    @Column(length = 100)
    private String referenciaExterna;

    @Column(length = 300)
    private String motivo;

    @Column(nullable = false)
    private Instant creadoEn;

    @Column(nullable = false)
    private Instant actualizadoEn;

    @Version
    private Long version;

    protected Pago() {
    }

    public Pago(Long pedidoId, String metodo, BigDecimal monto) {
        this.pedidoId = pedidoId;
        this.metodo = metodo;
        this.monto = monto;
        this.estado = EstadoPago.PENDIENTE;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    public void registrarIntento() {
        this.intentos++;
        this.actualizadoEn = Instant.now();
    }

    public void aprobar(String referencia) {
        this.estado = EstadoPago.APROBADO;
        this.referenciaExterna = referencia;
        this.motivo = null;
        this.actualizadoEn = Instant.now();
    }

    public void rechazar(String motivo) {
        this.estado = EstadoPago.RECHAZADO;
        this.motivo = motivo;
        this.actualizadoEn = Instant.now();
    }

    public boolean estaPendiente() {
        return estado == EstadoPago.PENDIENTE;
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public String getMetodo() { return metodo; }
    public BigDecimal getMonto() { return monto; }
    public EstadoPago getEstado() { return estado; }
    public int getIntentos() { return intentos; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public String getMotivo() { return motivo; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
