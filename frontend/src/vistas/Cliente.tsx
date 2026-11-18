import { useRef, useState } from 'react';
import { Link, NavLink, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { api, EstadoPedido, mensajeError, nuevaClave, Producto } from '../lib/api';
import { useConsulta, usePreferencia } from '../lib/hooks';
import { useUbicacionPedido } from '../lib/tracking';
import { dinero, fechaHora } from '../lib/formato';
import { EstadoCarga } from '../componentes/Estado';
import { EtiquetaEstado, LineaTiempo } from '../componentes/EstadoPedido';
import { Mapa, Punto } from '../componentes/Mapa';

const COSTO_ENVIO = 5000; // el valor definitivo lo calcula el backend

export default function Cliente() {
  const [clienteId, setClienteId] = usePreferencia('clienteId', 42);
  return (
    <>
      <div className="subbarra">
        <nav className="pestanas">
          <NavLink to="/cliente" end>Restaurantes</NavLink>
          <NavLink to="/cliente/pedidos">Mis pedidos</NavLink>
        </nav>
        <label className="selector">
          Cliente #
          <input type="number" min={1} value={clienteId} onChange={(e) => setClienteId(Number(e.target.value) || 1)} />
        </label>
      </div>
      <Routes>
        <Route index element={<ListaRestaurantes />} />
        <Route path="restaurantes/:id" element={<MenuYCarrito clienteId={clienteId} />} />
        <Route path="pedidos" element={<MisPedidos clienteId={clienteId} />} />
        <Route path="pedidos/:id" element={<SeguimientoPedido />} />
      </Routes>
    </>
  );
}

function ListaRestaurantes() {
  const { datos, error, cargando } = useConsulta(api.restaurantes, []);
  return (
    <>
      <h1>¿Qué quieres pedir hoy?</h1>
      <EstadoCarga cargando={cargando} error={error} />
      <div className="rejilla-tarjetas">
        {datos?.map((r) => (
          <Link key={r.id} to={`/cliente/restaurantes/${r.id}`} className={`tarjeta tarjeta-restaurante ${r.abierto ? '' : 'apagada'}`}>
            <h3>{r.nombre}</h3>
            <p className="suave">{r.direccion}</p>
            <span className={`etiqueta ${r.abierto ? 'tono-exito' : 'tono-error'}`}>{r.abierto ? 'Abierto' : 'Cerrado'}</span>
          </Link>
        ))}
      </div>
    </>
  );
}

function MenuYCarrito({ clienteId }: { clienteId: number }) {
  const restauranteId = Number(useParams().id);
  const restaurante = useConsulta(() => api.restaurante(restauranteId), [restauranteId]);
  const menu = useConsulta(() => api.menu(restauranteId), [restauranteId]);
  const metodos = useConsulta(api.metodosPago, []);
  const [carrito, setCarrito] = useState<Record<number, number>>({});
  const [metodo, setMetodo] = useState('TARJETA');
  const [direccion, setDireccion] = useState('Calle 93 # 11-30, Bogotá');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // La misma clave se reenvía si el cliente reintenta: el backend no crea un pedido duplicado.
  const clave = useRef(nuevaClave());
  const navegar = useNavigate();

  const productos = menu.datos ?? [];
  const lineas = productos.filter((p) => carrito[p.id]).map((p) => ({ producto: p, cantidad: carrito[p.id] }));
  const subtotal = lineas.reduce((suma, l) => suma + l.producto.precio * l.cantidad, 0);

  function cambiar(producto: Producto, delta: number) {
    clave.current = nuevaClave();
    setCarrito((actual) => {
      const cantidad = (actual[producto.id] ?? 0) + delta;
      const copia = { ...actual };
      if (cantidad <= 0) delete copia[producto.id];
      else copia[producto.id] = cantidad;
      return copia;
    });
  }

  async function confirmar() {
    setEnviando(true);
    setError(null);
    try {
      const pedido = await api.crearPedido({
        clienteId, restauranteId, metodoPago: metodo, direccionEntrega: direccion,
        items: lineas.map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad })),
      }, clave.current);
      navegar(`/cliente/pedidos/${pedido.id}`);
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <Link to="/cliente" className="volver">← Restaurantes</Link>
      <h1>{restaurante.datos?.nombre ?? 'Menú'}</h1>
      {restaurante.datos && <p className="suave">{restaurante.datos.direccion}</p>}
      {restaurante.datos && !restaurante.datos.abierto && <div className="aviso aviso-error">Este restaurante está cerrado en este momento.</div>}
      <div className="dos-columnas">
        <section>
          <EstadoCarga cargando={menu.cargando} error={menu.error} />
          <ul className="lista-productos">
            {productos.map((p) => (
              <li key={p.id} className={`tarjeta producto ${p.disponible ? '' : 'apagada'}`}>
                <div>
                  <h3>{p.nombre}</h3>
                  {p.descripcion && <p className="suave">{p.descripcion}</p>}
                  <strong>{dinero(p.precio)}</strong>
                </div>
                {!p.disponible ? (
                  <span className="etiqueta tono-error">Agotado</span>
                ) : carrito[p.id] ? (
                  <div className="contador">
                    <button onClick={() => cambiar(p, -1)} aria-label="Quitar uno">−</button>
                    <span>{carrito[p.id]}</span>
                    <button onClick={() => cambiar(p, 1)} aria-label="Agregar uno">+</button>
                  </div>
                ) : (
                  <button className="boton" onClick={() => cambiar(p, 1)}>Agregar</button>
                )}
              </li>
            ))}
          </ul>
        </section>

        <aside className="tarjeta carrito">
          <h2>Tu carrito</h2>
          {lineas.length === 0 ? (
            <p className="suave">Agrega productos del menú.</p>
          ) : (
            <>
              <ul className="lineas">
                {lineas.map((l) => (
                  <li key={l.producto.id}>
                    <span>{l.cantidad} × {l.producto.nombre}</span>
                    <span>{dinero(l.producto.precio * l.cantidad)}</span>
                  </li>
                ))}
              </ul>
              <dl className="totales">
                <dt>Subtotal</dt><dd>{dinero(subtotal)}</dd>
                <dt>Envío</dt><dd>{dinero(COSTO_ENVIO)}</dd>
                <dt className="total">Total</dt><dd className="total">{dinero(subtotal + COSTO_ENVIO)}</dd>
              </dl>
              <label className="campo">
                Dirección de entrega
                <input value={direccion} onChange={(e) => setDireccion(e.target.value)} />
              </label>
              <fieldset className="campo">
                <legend>Método de pago</legend>
                {metodos.datos?.map((m) => (
                  <label key={m.codigo} className="opcion">
                    <input type="radio" name="metodo" checked={metodo === m.codigo} onChange={() => setMetodo(m.codigo)} />
                    {m.descripcion}
                  </label>
                ))}
              </fieldset>
              {error && <div className="aviso aviso-error">{error}</div>}
              <button className="boton boton-ancho" disabled={enviando || !direccion.trim()} onClick={confirmar}>
                {enviando ? 'Enviando…' : 'Confirmar y pagar'}
              </button>
            </>
          )}
        </aside>
      </div>
    </>
  );
}

function MisPedidos({ clienteId }: { clienteId: number }) {
  const { datos, error, cargando } = useConsulta(() => api.pedidosDeCliente(clienteId), [clienteId], 5000);
  return (
    <>
      <h1>Mis pedidos</h1>
      <EstadoCarga cargando={cargando} error={error} />
      {datos && datos.length === 0 && <p className="suave">Todavía no has hecho pedidos con el cliente #{clienteId}.</p>}
      <ul className="lista-pedidos">
        {datos?.map((p) => (
          <li key={p.id}>
            <Link to={`/cliente/pedidos/${p.id}`} className="tarjeta fila-pedido">
              <strong>Pedido #{p.id}</strong>
              <span className="suave">{fechaHora(p.creadoEn)}</span>
              <span>{p.items.map((i) => `${i.cantidad} × ${i.nombre}`).join(', ')}</span>
              <span>{dinero(p.total)}</span>
              <EtiquetaEstado estado={p.estado} />
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}

const EN_RUTA: EstadoPedido[] = ['ASIGNADO', 'EN_CAMINO'];

function SeguimientoPedido() {
  const pedidoId = Number(useParams().id);
  const pedido = useConsulta(() => api.pedido(pedidoId), [pedidoId], 2000);
  const p = pedido.datos;
  const pago = useConsulta(() => api.pagoDePedido(pedidoId), [pedidoId], p?.estado === 'PAGO_PENDIENTE' || !p ? 2000 : 15000);
  const restaurante = useConsulta(
    () => (p ? api.restaurante(p.restauranteId) : Promise.resolve(null)), [p?.restauranteId]);
  const { ubicacion, conectado } = useUbicacionPedido(pedidoId, !!p && EN_RUTA.includes(p.estado));

  if (!p) return <EstadoCarga cargando={pedido.cargando} error={pedido.error} />;

  const puntos: Punto[] = [];
  if (restaurante.datos) {
    puntos.push({ id: 'r', lat: restaurante.datos.lat, lng: restaurante.datos.lng, tipo: 'restaurante', etiqueta: restaurante.datos.nombre });
  }
  const repartidor: Punto | undefined = ubicacion
    ? { id: 'd', lat: ubicacion.lat, lng: ubicacion.lng, tipo: 'repartidor', etiqueta: `Repartidor #${ubicacion.repartidorId}` }
    : undefined;
  if (repartidor) puntos.push(repartidor);

  return (
    <>
      <Link to="/cliente/pedidos" className="volver">← Mis pedidos</Link>
      <div className="encabezado-pedido">
        <h1>Pedido #{p.id}</h1>
        <EtiquetaEstado estado={p.estado} />
      </div>
      <LineaTiempo estado={p.estado} />

      {p.estado === 'PAGO_PENDIENTE' && (
        <div className="aviso aviso-espera">
          Estamos confirmando tu pago con la pasarela{pago.datos && pago.datos.intentos > 1 ? ` (intento ${pago.datos.intentos})` : ''}.
          Tu pedido ya quedó registrado; no tienes que hacer nada más.
        </div>
      )}
      {p.estado === 'PAGO_FALLIDO' && (
        <div className="aviso aviso-error">No pudimos cobrar el pedido: {pago.datos?.motivo ?? 'pago rechazado'}.</div>
      )}
      {p.estado === 'CANCELADO' && <div className="aviso aviso-error">El restaurante canceló este pedido.</div>}

      <div className="dos-columnas">
        <section className="tarjeta">
          <div className="titulo-con-estado">
            <h2>Seguimiento</h2>
            {EN_RUTA.includes(p.estado) && (
              <span className={`en-vivo ${conectado ? 'activo' : ''}`}>{conectado ? 'En vivo' : 'Conectando…'}</span>
            )}
          </div>
          <Mapa puntos={puntos} seguir={repartidor} />
          <p className="suave pie-mapa">
            {EN_RUTA.includes(p.estado)
              ? ubicacion ? 'La posición del repartidor llega por WebSocket cada 5 s.' : 'Esperando la primera ubicación del repartidor…'
              : 'El mapa mostrará al repartidor cuando se le asigne el pedido.'}
          </p>
        </section>
        <aside className="tarjeta">
          <h2>Detalle</h2>
          <ul className="lineas">
            {p.items.map((i) => (
              <li key={i.productoId}><span>{i.cantidad} × {i.nombre}</span><span>{dinero(i.subtotal)}</span></li>
            ))}
          </ul>
          <dl className="totales">
            <dt>Subtotal</dt><dd>{dinero(p.subtotal)}</dd>
            <dt>Envío</dt><dd>{dinero(p.costoEnvio)}</dd>
            <dt className="total">Total</dt><dd className="total">{dinero(p.total)}</dd>
          </dl>
          <dl className="datos">
            <dt>Entrega en</dt><dd>{p.direccionEntrega}</dd>
            <dt>Pago</dt><dd>{p.metodoPago}{pago.datos?.referencia ? ` · ${pago.datos.referencia}` : ''}</dd>
            {p.repartidorId && <><dt>Repartidor</dt><dd>#{p.repartidorId}</dd></>}
          </dl>
        </aside>
      </div>
    </>
  );
}
