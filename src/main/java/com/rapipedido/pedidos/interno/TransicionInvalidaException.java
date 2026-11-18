package com.rapipedido.pedidos.interno;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(EstadoPedido actual, EstadoPedido destino) {
        super("Un pedido en estado " + actual + " no puede pasar a " + destino);
    }
}
