import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { api, ReporteUbicacion, Ubicacion } from './api';

function urlWebSocket(): string {
  const protocolo = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocolo}://${location.host}/ws`;
}

/** Cliente: se suscribe por WebSocket a la ubicación del repartidor que lleva su pedido. */
export function useUbicacionPedido(pedidoId: number | undefined, activo: boolean) {
  const [ubicacion, setUbicacion] = useState<Ubicacion | null>(null);
  const [conectado, setConectado] = useState(false);

  useEffect(() => {
    if (!pedidoId || !activo) return;
    let vigente = true;
    // Última posición conocida, por si el repartidor no ha enviado nada desde que abrimos la vista
    api.ultimaUbicacion(pedidoId).then((u) => { if (vigente && u) setUbicacion(u); }).catch(() => {});

    const cliente = new Client({ brokerURL: urlWebSocket(), reconnectDelay: 2000 });
    cliente.onConnect = () => {
      setConectado(true);
      cliente.subscribe(`/topic/pedidos/${pedidoId}/ubicacion`, (mensaje) => setUbicacion(JSON.parse(mensaje.body)));
    };
    cliente.onWebSocketClose = () => setConectado(false);
    cliente.activate();
    return () => {
      vigente = false;
      setConectado(false);
      void cliente.deactivate();
    };
  }, [pedidoId, activo]);

  return { ubicacion, conectado };
}

/** Repartidor: publica su posición por WebSocket; si el socket está caído usa HTTP como respaldo. */
export function useEmisorUbicacion() {
  const clienteRef = useRef<Client | null>(null);
  const [conectado, setConectado] = useState(false);

  useEffect(() => {
    const cliente = new Client({ brokerURL: urlWebSocket(), reconnectDelay: 2000 });
    cliente.onConnect = () => setConectado(true);
    cliente.onWebSocketClose = () => setConectado(false);
    cliente.activate();
    clienteRef.current = cliente;
    return () => {
      clienteRef.current = null;
      void cliente.deactivate();
    };
  }, []);

  const enviar = useCallback((reporte: ReporteUbicacion) => {
    const cliente = clienteRef.current;
    if (cliente?.connected) {
      cliente.publish({ destination: '/app/ubicacion', body: JSON.stringify(reporte) });
    } else {
      api.reportarUbicacion(reporte).catch(() => {});
    }
  }, []);

  return { conectado, enviar };
}
