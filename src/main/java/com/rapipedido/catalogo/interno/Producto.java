package com.rapipedido.catalogo.interno;

import java.math.BigDecimal;

import com.rapipedido.catalogo.ProductoInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restauranteId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    private boolean disponible;

    protected Producto() {
    }

    public Producto(Long restauranteId, String nombre, String descripcion, BigDecimal precio) {
        this.restauranteId = restauranteId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = true;
    }

    public void actualizar(BigDecimal nuevoPrecio, Boolean nuevaDisponibilidad) {
        if (nuevoPrecio != null) {
            this.precio = nuevoPrecio;
        }
        if (nuevaDisponibilidad != null) {
            this.disponible = nuevaDisponibilidad;
        }
    }

    public ProductoInfo aInfo() {
        return new ProductoInfo(id, restauranteId, nombre, descripcion, precio, disponible);
    }
}
