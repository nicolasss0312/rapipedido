// Cliente de la API REST del monolito. Los tipos reflejan los DTO de Spring Boot.

export type EstadoPedido =
  | 'PAGO_PENDIENTE' | 'PAGADO' | 'PAGO_FALLIDO' | 'ACEPTADO' | 'EN_PREPARACION'
  | 'LISTO' | 'ASIGNADO' | 'EN_CAMINO' | 'ENTREGADO' | 'CANCELADO';

export interface Restaurante { id: number; nombre: string; direccion: string; lat: number; lng: number; abierto: boolean }
export interface Producto { id: number; restauranteId: number; nombre: string; descripcion: string | null; precio: number; disponible: boolean }
export interface ItemPedido { productoId: number; nombre: string; precioUnitario: number; cantidad: number; subtotal: number }
export interface Pedido {
  id: number; clienteId: number; restauranteId: number; estado: EstadoPedido; metodoPago: string;
  direccionEntrega: string; subtotal: number; costoEnvio: number; comision: number; total: number;
  repartidorId: number | null; creadoEn: string; actualizadoEn: string; items: ItemPedido[];
}
export interface NuevoPedido {
  clienteId: number; restauranteId: number; metodoPago: string; direccionEntrega: string;
  items: { productoId: number; cantidad: number }[];
}
export interface Pago {
  id: number; pedidoId: number; metodo: string; monto: number; estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';
  intentos: number; referencia: string | null; motivo: string | null; actualizadoEn: string;
}
export interface MetodoPago { codigo: string; descripcion: string }
export interface Repartidor { id: number; nombre: string; telefono: string; estado: 'DISPONIBLE' | 'OCUPADO' | 'DESCONECTADO' }
export interface Entrega {
  pedidoId: number; restauranteId: number; repartidorId: number | null;
  estado: 'PENDIENTE' | 'ASIGNADA' | 'RECOGIDA' | 'ENTREGADA'; latRecogida: number; lngRecogida: number;
}
export interface ReporteUbicacion { repartidorId: number; pedidoId: number | null; lat: number; lng: number }
export interface Ubicacion { repartidorId: number; pedidoId: number | null; lat: number; lng: number; momento: string | number }
export interface ConfigPasarela { caida: boolean; tasaFallo: number; tasaRechazo: number; latenciaMs: number }

export class ErrorApi extends Error {
  constructor(public readonly status: number, mensaje: string) {
    super(mensaje);
  }
}

async function pedir<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const respuesta = await fetch('/api' + ruta, {
    ...opciones,
    headers: { 'Content-Type': 'application/json', ...(opciones.headers ?? {}) },
  });
  if (!respuesta.ok) {
    let mensaje = `Error ${respuesta.status}`;
    try {
      const cuerpo = await respuesta.json();
      mensaje = cuerpo.detail ?? cuerpo.message ?? cuerpo.error ?? mensaje;
    } catch {
      // la respuesta no era JSON (por ejemplo, un 502 de nginx)
    }
    throw new ErrorApi(respuesta.status, mensaje);
  }
  return respuesta.status === 204 ? (undefined as T) : respuesta.json();
}

/** Igual que pedir, pero un 404 se interpreta como "no hay". */
async function pedirOpcional<T>(ruta: string): Promise<T | null> {
  try {
    return await pedir<T>(ruta);
  } catch (e) {
    if (e instanceof ErrorApi && e.status === 404) return null;
    throw e;
  }
}

const json = (metodo: string, cuerpo: unknown, headers?: Record<string, string>): RequestInit =>
  ({ method: metodo, body: JSON.stringify(cuerpo), headers });

export const api = {
  // Catálogo
  restaurantes: () => pedir<Restaurante[]>('/restaurantes'),
  restaurante: (id: number) => pedir<Restaurante>(`/restaurantes/${id}`),
  menu: (restauranteId: number) => pedir<Producto[]>(`/restaurantes/${restauranteId}/menu`),
  agregarProducto: (restauranteId: number, p: { nombre: string; descripcion: string; precio: number }) =>
    pedir<Producto>(`/restaurantes/${restauranteId}/productos`, json('POST', p)),
  actualizarProducto: (id: number, cambio: { precio?: number; disponible?: boolean }) =>
    pedir<Producto>(`/productos/${id}`, json('PATCH', cambio)),
  cambiarApertura: (restauranteId: number, abierto: boolean) =>
    pedir<Restaurante>(`/restaurantes/${restauranteId}/apertura`, json('PATCH', { abierto })),

  // Pedidos
  crearPedido: (pedido: NuevoPedido, claveIdempotencia: string) =>
    pedir<Pedido>('/pedidos', json('POST', pedido, { 'Idempotency-Key': claveIdempotencia })),
  pedido: (id: number) => pedir<Pedido>(`/pedidos/${id}`),
  pedidosDeCliente: (clienteId: number) => pedir<Pedido[]>(`/pedidos?clienteId=${clienteId}`),
  pedidosDeRestaurante: (restauranteId: number) => pedir<Pedido[]>(`/pedidos?restauranteId=${restauranteId}`),
  cambiarEstado: (id: number, estado: EstadoPedido) => pedir<Pedido>(`/pedidos/${id}/estado`, json('PATCH', { estado })),
  resumen: () => pedir<Record<EstadoPedido, number>>('/pedidos/resumen'),

  // Pagos
  metodosPago: () => pedir<MetodoPago[]>('/pagos/metodos'),
  pagoDePedido: (pedidoId: number) => pedirOpcional<Pago>(`/pagos/pedido/${pedidoId}`),
  pasarela: () => pedir<ConfigPasarela>('/admin/pasarela'),
  configurarPasarela: (config: ConfigPasarela) => pedir<ConfigPasarela>('/admin/pasarela', json('PUT', config)),

  // Asignación
  repartidores: () => pedir<Repartidor[]>('/repartidores'),
  cambiarDisponibilidad: (id: number, disponible: boolean) =>
    pedir<Repartidor>(`/repartidores/${id}/disponibilidad`, json('POST', { disponible })),
  entregaActual: (repartidorId: number) => pedirOpcional<Entrega>(`/repartidores/${repartidorId}/entrega-actual`),
  recoger: (pedidoId: number) => pedir<Entrega>(`/entregas/${pedidoId}/recoger`, { method: 'POST' }),
  confirmarEntrega: (pedidoId: number) => pedir<Entrega>(`/entregas/${pedidoId}/confirmar`, { method: 'POST' }),

  // Tracking
  reportarUbicacion: (r: ReporteUbicacion) => pedir<Ubicacion>('/tracking/ubicacion', json('POST', r)),
  ultimaUbicacion: (pedidoId: number) => pedirOpcional<Ubicacion>(`/tracking/pedidos/${pedidoId}/ubicacion`),
};

export function mensajeError(e: unknown): string {
  return e instanceof Error ? e.message : 'Ocurrió un error inesperado';
}

export function nuevaClave(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
