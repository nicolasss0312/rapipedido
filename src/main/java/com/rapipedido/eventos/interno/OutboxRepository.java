package com.rapipedido.eventos.interno;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvento, Long> {

    /**
     * Toma un lote de eventos pendientes. {@code FOR UPDATE SKIP LOCKED} permite que varias réplicas
     * del monolito ejecuten el relay a la vez sin enviar dos veces el mismo evento.
     */
    @Query(value = """
            select * from outbox_eventos
            where publicado_en is null
            order by id
            limit :limite
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvento> bloquearPendientes(@Param("limite") int limite);

    @Modifying
    @Query("delete from OutboxEvento e where e.publicadoEn < :limite")
    int borrarPublicadosAntesDe(@Param("limite") Instant limite);
}
