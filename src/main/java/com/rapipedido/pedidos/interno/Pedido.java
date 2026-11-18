package com.rapipedido.pedidos.interno;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Long restauranteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado;

    @Column(nullable = false, length = 30)
    private String metodoPago;

    @Column(nullable = false, length = 200)
    private String direccionEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEnvio;

    /** Comisión de la plataforma (se calcula con {@link PoliticaComision}). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal comision;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    private Long repartidorId;

    @Column(length = 100, unique = true)
    private String claveIdempotencia;

    @Column(nullable = false)
    private Instant creadoEn;

    @Column(nullable = false)
    private Instant actualizadoEn;

    @Version
    private Long version;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> items = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Long clienteId, Long restauranteId, String metodoPago, String direccionEntrega, String claveIdempotencia) {
        this.clienteId = clienteId;
        this.restauranteId = restauranteId;
        this.metodoPago = metodoPago;
        this.direccionEntrega = direccionEntrega;
        this.claveIdempotencia = claveIdempotencia;
        this.estado = EstadoPedido.PAGO_PENDIENTE;
        this.subtotal = BigDecimal.ZERO;
        this.costoEnvio = BigDecimal.ZERO;
        this.comision = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    public void agregarItem(Long productoId, String nombre, BigDecimal precioUnitario, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        ItemPedido item = new ItemPedido(this, productoId, nombre, precioUnitario, cantidad);
        items.add(item);
        subtotal = subtotal.add(item.getSubtotal());
    }

    /** Fija costos finales. El cliente paga subtotal + envío; la comisión la paga el restaurante. */
    public void liquidar(BigDecimal costoEnvio, BigDecimal comision) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un producto");
        }
        this.costoEnvio = costoEnvio;
        this.comision = comision;
        this.total = subtotal.add(costoEnvio);
    }

    /** @return el estado anterior */
    public EstadoPedido cambiarEstado(EstadoPedido nuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new TransicionInvalidaException(estado, nuevo);
        }
        EstadoPedido anterior = estado;
        estado = nuevo;
        actualizadoEn = Instant.now();
        return anterior;
    }

    public void asignarRepartidor(Long repartidorId) {
        this.repartidorId = repartidorId;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public Long getRestauranteId() { return restauranteId; }
    public EstadoPedido getEstado() { return estado; }
    public String getMetodoPago() { return metodoPago; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public BigDecimal getComision() { return comision; }
    public BigDecimal getTotal() { return total; }
    public Long getRepartidorId() { return repartidorId; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public List<ItemPedido> getItems() { return Collections.unmodifiableList(items); }
}
