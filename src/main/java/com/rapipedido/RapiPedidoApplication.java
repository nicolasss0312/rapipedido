package com.rapipedido;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RapiPedido: monolito modular con eventos (ADR-001).
 *
 * <p>Cada subpaquete directo es un módulo: catalogo, pedidos, pagos, asignacion, tracking,
 * notificaciones y eventos. Lo que está en la raíz de cada módulo es su API pública; lo que está
 * en {@code interno} solo lo puede usar el propio módulo. {@code ModularidadTest} hace fallar el
 * build si alguien rompe esa regla.
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class RapiPedidoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RapiPedidoApplication.class, args);
    }
}
