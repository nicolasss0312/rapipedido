import { useEffect } from 'react';
import { MapContainer, Marker, TileLayer, Tooltip, useMap } from 'react-leaflet';
import L from 'leaflet';

export interface Punto {
  id: string;
  lat: number;
  lng: number;
  tipo: 'restaurante' | 'repartidor' | 'destino';
  etiqueta: string;
}

const iconos = {
  restaurante: L.divIcon({ className: '', html: '<div class="pin pin-restaurante"></div>', iconSize: [22, 22], iconAnchor: [11, 11] }),
  repartidor: L.divIcon({ className: '', html: '<div class="pin pin-repartidor"></div>', iconSize: [22, 22], iconAnchor: [11, 11] }),
  destino: L.divIcon({ className: '', html: '<div class="pin pin-destino"></div>', iconSize: [22, 22], iconAnchor: [11, 11] }),
};

const BOGOTA: [number, number] = [4.66, -74.06];

function Seguir({ punto }: { punto?: Punto }) {
  const mapa = useMap();
  useEffect(() => {
    if (punto) mapa.panTo([punto.lat, punto.lng]);
  }, [mapa, punto?.lat, punto?.lng]); // eslint-disable-line react-hooks/exhaustive-deps
  return null;
}

/** Mapa de OpenStreetMap con los puntos del pedido; sigue al punto indicado en "seguir". */
export function Mapa({ puntos, seguir }: { puntos: Punto[]; seguir?: Punto }) {
  const centro: [number, number] = puntos.length ? [puntos[0].lat, puntos[0].lng] : BOGOTA;
  return (
    <MapContainer center={centro} zoom={14} scrollWheelZoom={false} className="mapa">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {puntos.map((p) => (
        <Marker key={p.id} position={[p.lat, p.lng]} icon={iconos[p.tipo]}>
          <Tooltip direction="top" offset={[0, -10]}>{p.etiqueta}</Tooltip>
        </Marker>
      ))}
      <Seguir punto={seguir} />
    </MapContainer>
  );
}
