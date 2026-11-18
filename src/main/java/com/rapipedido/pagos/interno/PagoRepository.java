package com.rapipedido.pagos.interno;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByPedidoId(Long pedidoId);

    List<Pago> findTop50ByEstadoAndActualizadoEnBeforeOrderByActualizadoEnAsc(EstadoPago estado, Instant antes);
}
