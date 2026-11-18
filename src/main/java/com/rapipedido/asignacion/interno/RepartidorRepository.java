package com.rapipedido.asignacion.interno;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    Optional<Repartidor> findFirstByEstadoOrderByIdAsc(EstadoRepartidor estado);
}
