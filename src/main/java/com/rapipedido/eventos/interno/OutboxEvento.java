package com.rapipedido.eventos.interno;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_eventos")
public class OutboxEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant creadoEn;

    private Instant publicadoEn;

    protected OutboxEvento() {
    }

    public OutboxEvento(String tipo, String payload) {
        this.tipo = tipo;
        this.payload = payload;
        this.creadoEn = Instant.now();
    }

    void marcarPublicado(Instant momento) {
        this.publicadoEn = momento;
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPayload() {
        return payload;
    }
}
