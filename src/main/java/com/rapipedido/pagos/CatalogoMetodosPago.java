package com.rapipedido.pagos;

import java.util.Set;

/** Lo único que otros módulos necesitan saber de pagos: qué métodos existen. */
public interface CatalogoMetodosPago {

    boolean soporta(String codigo);

    Set<String> disponibles();
}
