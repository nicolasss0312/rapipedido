package com.rapipedido.pagos.interno;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rapipedido.pagos.CatalogoMetodosPago;
import org.springframework.stereotype.Component;

/** Descubre todos los {@link MetodoPago} registrados como beans de Spring. */
@Component
public class RegistroMetodosPago implements CatalogoMetodosPago {

    private final Map<String, MetodoPago> metodos;

    public RegistroMetodosPago(List<MetodoPago> metodos) {
        this.metodos = metodos.stream()
                .collect(Collectors.toUnmodifiableMap(m -> normalizar(m.codigo()), Function.identity()));
    }

    @Override
    public boolean soporta(String codigo) {
        return codigo != null && metodos.containsKey(normalizar(codigo));
    }

    @Override
    public Set<String> disponibles() {
        return new TreeSet<>(metodos.keySet());
    }

    public Optional<MetodoPago> obtener(String codigo) {
        return Optional.ofNullable(codigo).map(c -> metodos.get(normalizar(c)));
    }

    public Collection<MetodoPago> todos() {
        return metodos.values();
    }

    private static String normalizar(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
}
