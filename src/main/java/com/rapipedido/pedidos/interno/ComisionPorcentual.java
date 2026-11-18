package com.rapipedido.pedidos.interno;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ComisionPorcentual implements PoliticaComision {

    private final BigDecimal porcentaje;

    public ComisionPorcentual(@Value("${rapipedido.comision.porcentaje:0.15}") BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, Long restauranteId) {
        return subtotal.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
    }
}
