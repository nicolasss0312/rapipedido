package com.rapipedido.catalogo.interno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByRestauranteIdOrderById(Long restauranteId);
}
