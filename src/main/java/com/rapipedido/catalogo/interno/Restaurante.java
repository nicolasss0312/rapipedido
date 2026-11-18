package com.rapipedido.catalogo.interno;

import com.rapipedido.catalogo.RestauranteInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurantes")
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String direccion;

    private double lat;
    private double lng;
    private boolean abierto;

    protected Restaurante() {
    }

    public void cambiarApertura(boolean abierto) {
        this.abierto = abierto;
    }

    public RestauranteInfo aInfo() {
        return new RestauranteInfo(id, nombre, direccion, lat, lng, abierto);
    }
}
