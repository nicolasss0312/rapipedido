package com.rapipedido.asignacion.interno;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    Optional<Asignacion> findByPedidoId(Long pedidoId);

    Optional<Asignacion> findFirstByRepartidorIdAndEstadoIn(Long repartidorId, Collection<EstadoAsignacion> estados);

    @Query("select a.id from Asignacion a where a.estado = com.rapipedido.asignacion.interno.EstadoAsignacion.PENDIENTE order by a.id")
    List<Long> idsPendientes();
}
