package com.rapipedido.pagos.interno;

public record ResultadoCobro(boolean aprobado, String referencia, String motivo) {

    public static ResultadoCobro aprobado(String referencia) {
        return new ResultadoCobro(true, referencia, null);
    }

    public static ResultadoCobro rechazado(String motivo) {
        return new ResultadoCobro(false, null, motivo);
    }
}
