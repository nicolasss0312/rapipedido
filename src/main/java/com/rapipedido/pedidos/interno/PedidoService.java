package com.rapipedido.pedidos.interno;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rapipedido.catalogo.CatalogoApi;
import com.rapipedido.catalogo.ProductoInfo;
import com.rapipedido.catalogo.RestauranteInfo;
import com.rapipedido.eventos.EstadoPedidoCambiado;
import com.rapipedido.eventos.PedidoCreado;
import com.rapipedido.eventos.PedidoListo;
import com.rapipedido.eventos.PublicadorEventos;
import com.rapipedido.pagos.CatalogoMetodosPago;
import com.rapipedido.pedidos.interno.Dtos.CrearPedidoRequest;
import com.rapipedido.pedidos.interno.Dtos.ItemRequest;
import com.rapipedido.pedidos.interno.Dtos.PedidoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidos;
    private final CatalogoApi catalogo;
    private final CatalogoMetodosPago metodosPago;
    private final PoliticaComision politicaComision;
    private final PublicadorEventos eventos;
    private final BigDecimal costoEnvio;

    public PedidoService(PedidoRepository pedidos, CatalogoApi catalogo, CatalogoMetodosPago metodosPago,
                         PoliticaComision politicaComision, PublicadorEventos eventos,
                         @Value("${rapipedido.costo-envio:5000}") BigDecimal costoEnvio) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.metodosPago = metodosPago;
        this.politicaComision = politicaComision;
        this.eventos = eventos;
        this.costoEnvio = costoEnvio;
    }

    /**
     * Registra el pedido en estado PAGO_PENDIENTE y deja el evento {@code PedidoCreado} en el outbox.
     * No llama a la pasarela: el cliente recibe respuesta aunque el proveedor de pagos esté lento o caído.
     */
    @Transactional
    public PedidoResponse crear(CrearPedidoRequest solicitud, String claveIdempotencia) {
        if (claveIdempotencia != null) {
            var existente = pedidos.findByClaveIdempotencia(claveIdempotencia);
            if (existente.isPresent()) {
                return PedidoResponse.de(existente.get());
            }
        }
        if (!metodosPago.soporta(solicitud.metodoPago())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Método de pago no soportado. Disponibles: " + metodosPago.disponibles());
        }
        RestauranteInfo restaurante = catalogo.restaurante(solicitud.restauranteId());
        if (!restaurante.abierto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El restaurante está cerrado");
        }

        Map<Long, ProductoInfo> menu = catalogo.menu(restaurante.id()).stream()
                .collect(Collectors.toMap(ProductoInfo::id, Function.identity()));

        Pedido pedido = new Pedido(solicitud.clienteId(), restaurante.id(),
                solicitud.metodoPago().trim().toUpperCase(Locale.ROOT), solicitud.direccionEntrega(), claveIdempotencia);
        for (ItemRequest item : solicitud.items()) {
            ProductoInfo producto = menu.get(item.productoId());
            if (producto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto " + item.productoId() + " no pertenece al restaurante " + restaurante.id());
            }
            if (!producto.disponible()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto '" + producto.nombre() + "' está agotado");
            }
            pedido.agregarItem(producto.id(), producto.nombre(), producto.precio(), item.cantidad());
        }
        pedido.liquidar(costoEnvio, politicaComision.calcular(pedido.getSubtotal(), restaurante.id()));

        pedidos.save(pedido);
        eventos.publicar(new PedidoCreado(pedido.getId(), pedido.getClienteId(), pedido.getRestauranteId(),
                pedido.getTotal(), pedido.getMetodoPago()));
        log.info("Pedido {} registrado (cliente {}, total {})", pedido.getId(), pedido.getClienteId(), pedido.getTotal());
        return PedidoResponse.de(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return PedidoResponse.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorClave(String claveIdempotencia) {
        return pedidos.findByClaveIdempotencia(claveIdempotencia).map(PedidoResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long clienteId, Long restauranteId) {
        List<Pedido> encontrados;
        if (clienteId != null) {
            encontrados = pedidos.findTop50ByClienteIdOrderByCreadoEnDesc(clienteId);
        } else if (restauranteId != null) {
            encontrados = pedidos.findTop50ByRestauranteIdOrderByCreadoEnDesc(restauranteId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique clienteId o restauranteId");
        }
        return encontrados.stream().map(PedidoResponse::de).toList();
    }

    /** Panel del restaurante: aceptar, preparar, marcar listo o cancelar. */
    @Transactional
    public PedidoResponse cambiarEstadoPorRestaurante(Long id, EstadoPedido nuevo) {
        if (!EstadoPedido.CONTROLADOS_POR_RESTAURANTE.contains(nuevo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El restaurante solo puede fijar: " + EstadoPedido.CONTROLADOS_POR_RESTAURANTE);
        }
        Pedido pedido = buscar(id);
        transicionar(pedido, nuevo);
        if (nuevo == EstadoPedido.LISTO) {
            RestauranteInfo restaurante = catalogo.restaurante(pedido.getRestauranteId());
            eventos.publicar(new PedidoListo(pedido.getId(), restaurante.id(), restaurante.lat(), restaurante.lng()));
        }
        return PedidoResponse.de(pedido);
    }

    /**
     * Aplica un cambio que viene de otro módulo por un evento. Es idempotente: si el mensaje llega
     * repetido o fuera de orden, se ignora en lugar de fallar.
     */
    @Transactional
    public void aplicarCambioExterno(Long pedidoId, EstadoPedido nuevo, Long repartidorId) {
        Pedido pedido = pedidos.findById(pedidoId).orElse(null);
        if (pedido == null) {
            log.warn("Evento para pedido inexistente {}", pedidoId);
            return;
        }
        if (pedido.getEstado() == nuevo) {
            return;
        }
        if (!pedido.getEstado().puedePasarA(nuevo)) {
            log.warn("Pedido {}: se ignora el paso de {} a {}", pedidoId, pedido.getEstado(), nuevo);
            return;
        }
        if (repartidorId != null) {
            pedido.asignarRepartidor(repartidorId);
        }
        transicionar(pedido, nuevo);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> resumenPorEstado() {
        Map<String, Long> resumen = new LinkedHashMap<>();
        for (EstadoPedido estado : EstadoPedido.values()) {
            resumen.put(estado.name(), 0L);
        }
        for (Object[] fila : pedidos.contarPorEstado()) {
            resumen.put(((EstadoPedido) fila[0]).name(), (Long) fila[1]);
        }
        return resumen;
    }

    private void transicionar(Pedido pedido, EstadoPedido nuevo) {
        EstadoPedido anterior = pedido.cambiarEstado(nuevo);
        eventos.publicar(new EstadoPedidoCambiado(pedido.getId(), pedido.getClienteId(), pedido.getRestauranteId(),
                anterior.name(), nuevo.name()));
    }

    private Pedido buscar(Long id) {
        return pedidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido " + id + " no existe"));
    }
}
