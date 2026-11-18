package com.rapipedido.pedidos.interno;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Copia el nombre y precio del producto al momento de la compra (el menú puede cambiar después). */
@Entity
@Table(name = "items_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    private int cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected ItemPedido() {
    }

    ItemPedido(Pedido pedido, Long productoId, String nombre, BigDecimal precioUnitario, int cantidad) {
        this.pedido = pedido;
        this.productoId = productoId;
        this.nombre = nombre;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getProductoId() { return productoId; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getSubtotal() { return subtotal; }
}
