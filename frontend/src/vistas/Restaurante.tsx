import { FormEvent, useState } from 'react';
import { NavLink, Route, Routes } from 'react-router-dom';
import { api, EstadoPedido, mensajeError, Pedido, Producto } from '../lib/api';
import { useConsulta, usePreferencia } from '../lib/hooks';
import { dinero, hora } from '../lib/formato';
import { EstadoCarga } from '../componentes/Estado';

export default function PanelRestaurante() {
  const restaurantes = useConsulta(api.restaurantes, []);
  const [restauranteId, setRestauranteId] = usePreferencia('restauranteId', 1);
  const actual = restaurantes.datos?.find((r) => r.id === restauranteId);
  const [error, setError] = useState<string | null>(null);

  async function alternarApertura() {
    if (!actual) return;
    try {
      await api.cambiarApertura(actual.id, !actual.abierto);
      await restaurantes.recargar();
    } catch (e) {
      setError(mensajeError(e));
    }
  }

  return (
    <>
      <div className="subbarra">
        <nav className="pestanas">
          <NavLink to="/restaurante" end>Pedidos</NavLink>
          <NavLink to="/restaurante/menu">Menú</NavLink>
        </nav>
        <div className="acciones-subbarra">
          <select value={restauranteId} onChange={(e) => setRestauranteId(Number(e.target.value))} aria-label="Restaurante">
            {restaurantes.datos?.map((r) => <option key={r.id} value={r.id}>{r.nombre}</option>)}
          </select>
          {actual && (
            <button className={`interruptor ${actual.abierto ? 'encendido' : ''}`} onClick={alternarApertura}>
              {actual.abierto ? 'Abierto' : 'Cerrado'}
            </button>
          )}
        </div>
      </div>
      {error && <div className="aviso aviso-error">{error}</div>}
      <Routes>
        <Route index element={<TableroPedidos restauranteId={restauranteId} />} />
        <Route path="menu" element={<GestionMenu restauranteId={restauranteId} />} />
      </Routes>
    </>
  );
}

interface Accion { etiqueta: string; estado: EstadoPedido; peligro?: boolean }
interface Columna { titulo: string; estados: EstadoPedido[]; acciones: Accion[] }

const COLUMNAS: Columna[] = [
  {
    titulo: 'Nuevos',
    estados: ['PAGADO'],
    acciones: [
      { etiqueta: 'Aceptar', estado: 'ACEPTADO' },
      { etiqueta: 'Rechazar', estado: 'CANCELADO', peligro: true },
    ],
  },
  {
    titulo: 'Aceptados',
    estados: ['ACEPTADO'],
    acciones: [
      { etiqueta: 'Empezar a preparar', estado: 'EN_PREPARACION' },
      { etiqueta: 'Cancelar', estado: 'CANCELADO', peligro: true },
    ],
  },
  {
    titulo: 'En preparación',
    estados: ['EN_PREPARACION'],
    acciones: [{ etiqueta: 'Marcar listo', estado: 'LISTO' }],
  },
  {
    titulo: 'Despacho',
    estados: ['LISTO', 'ASIGNADO', 'EN_CAMINO'],
    acciones: [], // solo informa: la asignación y la entrega llegan por eventos
  },
];

function textoDespacho(p: Pedido): string {
  if (p.estado === 'LISTO') return 'Buscando repartidor…';
  if (p.estado === 'ASIGNADO') return `Repartidor #${p.repartidorId} viene a recogerlo`;
  return `Repartidor #${p.repartidorId} lo está entregando`;
}

function TableroPedidos({ restauranteId }: { restauranteId: number }) {
  const pedidos = useConsulta(() => api.pedidosDeRestaurante(restauranteId), [restauranteId], 3000);
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const lista = pedidos.datos ?? [];
  const esperandoPago = lista.filter((p) => p.estado === 'PAGO_PENDIENTE').length;

  async function mover(pedido: Pedido, estado: EstadoPedido) {
    setOcupado(pedido.id);
    setError(null);
    try {
      await api.cambiarEstado(pedido.id, estado);
      await pedidos.recargar();
    } catch (e) {
      setError(`Pedido #${pedido.id}: ${mensajeError(e)}`);
    } finally {
      setOcupado(null);
    }
  }

  return (
    <>
      <div className="titulo-con-estado">
        <h1>Pedidos</h1>
        {esperandoPago > 0 && <span className="etiqueta tono-espera">{esperandoPago} esperando confirmación de pago</span>}
      </div>
      <EstadoCarga cargando={pedidos.cargando} error={pedidos.error} />
      {error && <div className="aviso aviso-error">{error}</div>}
      <div className="tablero">
        {COLUMNAS.map((columna) => {
          const enColumna = lista.filter((p) => columna.estados.includes(p.estado));
          return (
            <section key={columna.titulo} className="columna">
              <h2>{columna.titulo} <span className="contador-columna">{enColumna.length}</span></h2>
              {enColumna.length === 0 && <p className="suave vacio">Sin pedidos</p>}
              {enColumna.map((p) => (
                <article key={p.id} className="tarjeta comanda">
                  <header>
                    <strong>#{p.id}</strong>
                    <span className="suave">{hora(p.creadoEn)}</span>
                  </header>
                  <ul>
                    {p.items.map((i) => <li key={i.productoId}>{i.cantidad} × {i.nombre}</li>)}
                  </ul>
                  <footer>
                    <span>{dinero(p.subtotal)}</span>
                    {columna.acciones.length === 0 && <span className="suave">{textoDespacho(p)}</span>}
                  </footer>
                  {columna.acciones.length > 0 && (
                    <div className="botones">
                      {columna.acciones.map((a) => (
                        <button key={a.estado} disabled={ocupado === p.id}
                                className={a.peligro ? 'boton boton-secundario' : 'boton'}
                                onClick={() => mover(p, a.estado)}>
                          {a.etiqueta}
                        </button>
                      ))}
                    </div>
                  )}
                </article>
              ))}
            </section>
          );
        })}
      </div>
    </>
  );
}

function GestionMenu({ restauranteId }: { restauranteId: number }) {
  const menu = useConsulta(() => api.menu(restauranteId), [restauranteId]);
  const [error, setError] = useState<string | null>(null);
  const [nuevo, setNuevo] = useState({ nombre: '', descripcion: '', precio: '' });

  async function ejecutar(accion: () => Promise<unknown>) {
    setError(null);
    try {
      await accion();
      await menu.recargar();
    } catch (e) {
      setError(mensajeError(e));
    }
  }

  async function agregar(evento: FormEvent) {
    evento.preventDefault();
    await ejecutar(() => api.agregarProducto(restauranteId, {
      nombre: nuevo.nombre, descripcion: nuevo.descripcion, precio: Number(nuevo.precio),
    }));
    setNuevo({ nombre: '', descripcion: '', precio: '' });
  }

  return (
    <>
      <h1>Menú</h1>
      <p className="suave">Los cambios invalidan la caché de Redis: los clientes ven el menú actualizado de inmediato.</p>
      <EstadoCarga cargando={menu.cargando} error={menu.error} />
      {error && <div className="aviso aviso-error">{error}</div>}
      <div className="tarjeta">
        <table className="tabla">
          <thead>
            <tr><th>Producto</th><th>Precio</th><th>Disponibilidad</th></tr>
          </thead>
          <tbody>
            {menu.datos?.map((p) => (
              <FilaProducto key={p.id} producto={p}
                            onPrecio={(precio) => ejecutar(() => api.actualizarProducto(p.id, { precio }))}
                            onDisponible={(disponible) => ejecutar(() => api.actualizarProducto(p.id, { disponible }))} />
            ))}
          </tbody>
        </table>
      </div>
      <form className="tarjeta formulario-linea" onSubmit={agregar}>
        <h2>Agregar producto</h2>
        <input placeholder="Nombre" required value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} />
        <input placeholder="Descripción" value={nuevo.descripcion} onChange={(e) => setNuevo({ ...nuevo, descripcion: e.target.value })} />
        <input placeholder="Precio" type="number" min={1} required value={nuevo.precio} onChange={(e) => setNuevo({ ...nuevo, precio: e.target.value })} />
        <button className="boton">Agregar</button>
      </form>
    </>
  );
}

function FilaProducto({ producto, onPrecio, onDisponible }: {
  producto: Producto; onPrecio: (precio: number) => void; onDisponible: (disponible: boolean) => void;
}) {
  const [precio, setPrecio] = useState(String(producto.precio));
  const cambiado = Number(precio) !== producto.precio && Number(precio) > 0;
  return (
    <tr>
      <td>
        <strong>{producto.nombre}</strong>
        {producto.descripcion && <div className="suave">{producto.descripcion}</div>}
      </td>
      <td>
        <div className="precio-editable">
          <input type="number" min={1} value={precio} onChange={(e) => setPrecio(e.target.value)} aria-label={`Precio de ${producto.nombre}`} />
          {cambiado && <button className="boton boton-chico" onClick={() => onPrecio(Number(precio))}>Guardar</button>}
        </div>
      </td>
      <td>
        <button className={`interruptor ${producto.disponible ? 'encendido' : ''}`} onClick={() => onDisponible(!producto.disponible)}>
          {producto.disponible ? 'Disponible' : 'Agotado'}
        </button>
      </td>
    </tr>
  );
}
