import { EstadoPedido } from '../lib/api';

export const NOMBRE_ESTADO: Record<EstadoPedido, string> = {
  PAGO_PENDIENTE: 'Confirmando pago',
  PAGADO: 'Pagado',
  PAGO_FALLIDO: 'Pago fallido',
  ACEPTADO: 'Aceptado',
  EN_PREPARACION: 'En preparación',
  LISTO: 'Listo para recoger',
  ASIGNADO: 'Repartidor asignado',
  EN_CAMINO: 'En camino',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
};

const TONO: Record<EstadoPedido, string> = {
  PAGO_PENDIENTE: 'espera', PAGADO: 'progreso', PAGO_FALLIDO: 'error', ACEPTADO: 'progreso',
  EN_PREPARACION: 'progreso', LISTO: 'progreso', ASIGNADO: 'progreso', EN_CAMINO: 'progreso',
  ENTREGADO: 'exito', CANCELADO: 'error',
};

export function EtiquetaEstado({ estado }: { estado: EstadoPedido }) {
  return <span className={`etiqueta tono-${TONO[estado]}`}>{NOMBRE_ESTADO[estado]}</span>;
}

const PASOS: { estado: EstadoPedido; nombre: string }[] = [
  { estado: 'PAGO_PENDIENTE', nombre: 'Pago' },
  { estado: 'PAGADO', nombre: 'Pagado' },
  { estado: 'ACEPTADO', nombre: 'Aceptado' },
  { estado: 'EN_PREPARACION', nombre: 'Preparando' },
  { estado: 'LISTO', nombre: 'Listo' },
  { estado: 'ASIGNADO', nombre: 'Repartidor' },
  { estado: 'EN_CAMINO', nombre: 'En camino' },
  { estado: 'ENTREGADO', nombre: 'Entregado' },
];

/** Línea de tiempo del pedido para el cliente. */
export function LineaTiempo({ estado }: { estado: EstadoPedido }) {
  const actual = PASOS.findIndex((p) => p.estado === estado);
  return (
    <ol className="linea-tiempo">
      {PASOS.map((paso, i) => {
        const clase = actual < 0 ? '' : i < actual || estado === 'ENTREGADO' ? 'hecho' : i === actual ? 'actual' : '';
        return (
          <li key={paso.estado} className={clase}>
            <span className="punto" />
            <span className="nombre">{paso.nombre}</span>
          </li>
        );
      })}
    </ol>
  );
}
