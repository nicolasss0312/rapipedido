package com.rapipedido.tracking;

import java.util.List;

public interface UbicacionRepartidores {

    /** IDs de los repartidores que reportaron posición dentro del radio, del más cercano al más lejano. */
    List<Long> cercanos(double lat, double lng, double radioKm);
}
