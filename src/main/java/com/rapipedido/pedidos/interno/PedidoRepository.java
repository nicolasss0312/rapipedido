package com.rapipedido.pedidos.interno;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByClaveIdempotencia(String claveIdempotencia);

    List<Pedido> findTop50ByClienteIdOrderByCreadoEnDesc(Long clienteId);

    List<Pedido> findTop50ByRestauranteIdOrderByCreadoEnDesc(Long restauranteId);

    @Query("select p.estado, count(p) from Pedido p group by p.estado")
    List<Object[]> contarPorEstado();
}
