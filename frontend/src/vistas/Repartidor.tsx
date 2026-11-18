import { useEffect, useRef, useState } from 'react';
import { api, Entrega, mensajeError } from '../lib/api';
import { useConsulta, usePreferencia } from '../lib/hooks';
import { useEmisorUbicacion } from '../lib/tracking';
import { dinero } from '../lib/formato';
import { EstadoCarga } from '../componentes/Estado';
import { Mapa, Punto } from '../componentes/Mapa';

interface Posicion { lat: number; lng: number }

const INICIO_SIN_ENTREGA: Posicion = { lat: 4.6700, lng: -74.0550 };
const PASO_GRADOS = 0.0015; // ~165 m cada 5 s en la simulación

/** Punto de entrega simulado: el backend no geocodifica la dirección del cliente. */
const destinoCliente = (e: Entrega): Posicion => ({ lat: e.latRecogida - 0.010, lng: e.lngRecogida - 0.006 });

function avanzar(desde: Posicion, hacia: Posicion): Posicion {
  const dLat = hacia.lat - desde.lat;
  const dLng = hacia.lng - desde.lng;
  const distancia = Math.hypot(dLat, dLng);
  if (distancia <= PASO_GRADOS) return hacia;
  return { lat: desde.lat + (dLat / distancia) * PASO_GRADOS, lng: desde.lng + (dLng / distancia) * PASO_GRADOS };
}

export default function AppRepartidor() {
  const repartidores = useConsulta(api.repartidores, [], 3000);
  const [repartidorId, setRepartidorId] = usePreferencia('repartidorId', 1);
  const yo = repartidores.datos?.find((r) => r.id === repartidorId);
  const entrega = useConsulta(() => api.entregaActual(repartidorId), [repartidorId], 3000);
  const e = entrega.datos ?? null;
  const pedido = useConsulta(() => (e ? api.pedido(e.pedidoId) : Promise.resolve(null)), [e?.pedidoId]);
  const restaurante = useConsulta(() => (e ? api.restaurante(e.restauranteId) : Promise.resolve(null)), [e?.restauranteId]);

  const emisor = useEmisorUbicacion();
  const [compartiendo, setCompartiendo] = useState(true);
  const [gpsReal, setGpsReal] = useState(false);
  const [posicion, setPosicion] = useState<Posicion | null>(null);
  const [ultimoEnvio, setUltimoEnvio] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  // Refs para que el temporizador de 5 s lea siempre los datos más recientes sin reiniciarse.
  const entregaRef = useRef<Entrega | null>(null);
  entregaRef.current = e;
  const posicionRef = useRef<Posicion | null>(null);

  useEffect(() => {
    posicionRef.current = null;
    setPosicion(null);
  }, [repartidorId]);

  useEffect(() => {
    if (!compartiendo) return;
    const enviar = (p: Posicion) => {
      posicionRef.current = p;
      setPosicion(p);
      setUltimoEnvio(new Date());
      emisor.enviar({ repartidorId, pedidoId: entregaRef.current?.pedidoId ?? null, lat: p.lat, lng: p.lng });
    };
    const tick = () => {
      if (gpsReal) {
        navigator.geolocation.getCurrentPosition(
          (g) => enviar({ lat: g.coords.latitude, lng: g.coords.longitude }),
          () => setError('No se pudo leer el GPS del navegador; usa la ruta simulada.'),
        );
        return;
      }
      const actual = entregaRef.current;
      const desde = posicionRef.current
        ?? (actual ? { lat: actual.latRecogida + 0.006, lng: actual.lngRecogida + 0.004 } : INICIO_SIN_ENTREGA);
      if (!actual) {
        enviar(desde);
        return;
      }
      const objetivo = actual.estado === 'RECOGIDA'
        ? destinoCliente(actual)
        : { lat: actual.latRecogida, lng: actual.lngRecogida };
      enviar(avanzar(desde, objetivo));
    };
    tick();
    const temporizador = setInterval(tick, 5000);
    return () => clearInterval(temporizador);
  }, [compartiendo, gpsReal, repartidorId, emisor.enviar]); // eslint-disable-line react-hooks/exhaustive-deps

  async function ejecutar(accion: () => Promise<unknown>) {
    setOcupado(true);
    setError(null);
    try {
      await accion();
      await Promise.all([entrega.recargar(), repartidores.recargar()]);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setOcupado(false);
    }
  }

  const puntos: Punto[] = [];
  if (restaurante.datos) {
    puntos.push({ id: 'r', lat: restaurante.datos.lat, lng: restaurante.datos.lng, tipo: 'restaurante', etiqueta: restaurante.datos.nombre });
  }
  if (e?.estado === 'RECOGIDA') {
    const destino = destinoCliente(e);
    puntos.push({ id: 'c', ...destino, tipo: 'destino', etiqueta: 'Cliente' });
  }
  const yoEnMapa: Punto | undefined = posicion ? { id: 'yo', ...posicion, tipo: 'repartidor', etiqueta: 'Tú' } : undefined;
  if (yoEnMapa) puntos.push(yoEnMapa);

  return (
    <div className="telefono">
      <div className="tarjeta">
        <label className="campo">
          Soy el repartidor
          <select value={repartidorId} onChange={(ev) => setRepartidorId(Number(ev.target.value))}>
            {repartidores.datos?.map((r) => <option key={r.id} value={r.id}>#{r.id} · {r.nombre}</option>)}
          </select>
        </label>
        {yo && (
          <div className="fila-estado">
            <span className={`etiqueta ${yo.estado === 'DISPONIBLE' ? 'tono-exito' : yo.estado === 'OCUPADO' ? 'tono-progreso' : 'tono-apagado'}`}>
              {yo.estado === 'DISPONIBLE' ? 'Disponible' : yo.estado === 'OCUPADO' ? 'En una entrega' : 'Desconectado'}
            </span>
            {yo.estado !== 'OCUPADO' && (
              <button className="boton boton-secundario" disabled={ocupado}
                      onClick={() => ejecutar(() => api.cambiarDisponibilidad(yo.id, yo.estado !== 'DISPONIBLE'))}>
                {yo.estado === 'DISPONIBLE' ? 'Desconectarme' : 'Conectarme'}
              </button>
            )}
          </div>
        )}
      </div>

      {error && <div className="aviso aviso-error">{error}</div>}

      <div className="tarjeta">
        <h2>Entrega actual</h2>
        <EstadoCarga cargando={entrega.cargando} error={entrega.error} />
        {!entrega.cargando && !e && (
          <p className="suave">
            {yo?.estado === 'DISPONIBLE'
              ? 'Esperando pedidos. Cuando un restaurante marque un pedido como listo, se te asignará si eres el más cercano.'
              : 'Conéctate para recibir pedidos.'}
          </p>
        )}
        {e && (
          <>
            <p className="pedido-asignado">Pedido <strong>#{e.pedidoId}</strong></p>
            <dl className="datos">
              <dt>Recoger en</dt>
              <dd>{restaurante.datos ? `${restaurante.datos.nombre}, ${restaurante.datos.direccion}` : '…'}</dd>
              <dt>Entregar en</dt>
              <dd>{pedido.datos?.direccionEntrega ?? '…'}</dd>
              {pedido.datos && <><dt>Productos</dt><dd>{pedido.datos.items.map((i) => `${i.cantidad} × ${i.nombre}`).join(', ')}</dd></>}
              {pedido.datos?.metodoPago === 'EFECTIVO' && <><dt>Cobrar</dt><dd><strong>{dinero(pedido.datos.total)}</strong> en efectivo</dd></>}
            </dl>
            {e.estado === 'ASIGNADA' && (
              <button className="boton boton-ancho" disabled={ocupado} onClick={() => ejecutar(() => api.recoger(e.pedidoId))}>
                Ya recogí el pedido
              </button>
            )}
            {e.estado === 'RECOGIDA' && (
              <button className="boton boton-ancho" disabled={ocupado} onClick={() => ejecutar(() => api.confirmarEntrega(e.pedidoId))}>
                Confirmar entrega
              </button>
            )}
          </>
        )}
      </div>

      <div className="tarjeta">
        <div className="titulo-con-estado">
          <h2>Mi ubicación</h2>
          <span className={`en-vivo ${compartiendo && emisor.conectado ? 'activo' : ''}`}>
            {!compartiendo ? 'Pausada' : emisor.conectado ? 'WebSocket' : 'HTTP (respaldo)'}
          </span>
        </div>
        <Mapa puntos={puntos} seguir={yoEnMapa} />
        <p className="suave pie-mapa">
          {compartiendo
            ? `Enviando posición cada 5 s${ultimoEnvio ? ` · último envío ${ultimoEnvio.toLocaleTimeString('es-CO')}` : ''}`
            : 'No estás compartiendo tu ubicación.'}
        </p>
        <div className="fila-estado">
          <button className="boton boton-secundario" onClick={() => setCompartiendo(!compartiendo)}>
            {compartiendo ? 'Pausar GPS' : 'Compartir ubicación'}
          </button>
          <label className="opcion">
            <input type="checkbox" checked={gpsReal} onChange={(ev) => setGpsReal(ev.target.checked)} />
            GPS real del navegador
          </label>
        </div>
      </div>
    </div>
  );
}
