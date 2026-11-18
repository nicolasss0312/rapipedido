import { FormEvent, useEffect, useState } from 'react';
import { api, ConfigPasarela, EstadoPedido, mensajeError, Pago } from '../lib/api';
import { useConsulta } from '../lib/hooks';
import { dinero, fechaHora } from '../lib/formato';
import { EstadoCarga } from '../componentes/Estado';
import { NOMBRE_ESTADO } from '../componentes/EstadoPedido';

const ORDEN: EstadoPedido[] = ['PAGO_PENDIENTE', 'PAGADO', 'ACEPTADO', 'EN_PREPARACION', 'LISTO', 'ASIGNADO',
  'EN_CAMINO', 'ENTREGADO', 'PAGO_FALLIDO', 'CANCELADO'];

export default function PanelAdmin() {
  const resumen = useConsulta(api.resumen, [], 4000);
  const repartidores = useConsulta(api.repartidores, [], 4000);
  const total = resumen.datos ? Object.values(resumen.datos).reduce((a, b) => a + b, 0) : 0;

  return (
    <>
      <h1>Administración</h1>
      <section>
        <div className="titulo-con-estado">
          <h2>Pedidos por estado</h2>
          <span className="suave">{total} en total · se actualiza cada 4 s</span>
        </div>
        <EstadoCarga cargando={resumen.cargando} error={resumen.error} />
        <div className="rejilla-indicadores">
          {resumen.datos && ORDEN.map((estado) => (
            <div key={estado} className={`tarjeta indicador ${estado === 'PAGO_PENDIENTE' && resumen.datos![estado] > 0 ? 'resaltado' : ''}`}>
              <span className="valor">{resumen.datos![estado] ?? 0}</span>
              <span className="suave">{NOMBRE_ESTADO[estado]}</span>
            </div>
          ))}
        </div>
      </section>

      <div className="dos-columnas iguales">
        <ControlPasarela />
        <section className="tarjeta">
          <h2>Repartidores</h2>
          <table className="tabla">
            <thead><tr><th>#</th><th>Nombre</th><th>Estado</th></tr></thead>
            <tbody>
              {repartidores.datos?.map((r) => (
                <tr key={r.id}>
                  <td>{r.id}</td>
                  <td>{r.nombre}</td>
                  <td>
                    <span className={`etiqueta ${r.estado === 'DISPONIBLE' ? 'tono-exito' : r.estado === 'OCUPADO' ? 'tono-progreso' : 'tono-apagado'}`}>
                      {r.estado.toLowerCase()}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>

      <div className="dos-columnas iguales">
        <BuscarPago />
        <section className="tarjeta">
          <h2>Herramientas</h2>
          <ul className="enlaces">
            <li><a href="/swagger-ui.html" target="_blank" rel="noreferrer">Documentación de la API (Swagger)</a></li>
            <li><a href={`http://${location.hostname}:15672`} target="_blank" rel="noreferrer">Consola de RabbitMQ</a> (guest / guest): colas y DLQ</li>
            <li><a href="/actuator/health" target="_blank" rel="noreferrer">Salud del sistema</a> (base de datos, Redis, RabbitMQ)</li>
          </ul>
        </section>
      </div>
    </>
  );
}

/** Permite provocar en vivo el escenario de disponibilidad: la pasarela deja de responder. */
function ControlPasarela() {
  const pasarela = useConsulta(api.pasarela, [], 4000);
  const [formulario, setFormulario] = useState<ConfigPasarela | null>(null);
  const [error, setError] = useState<string | null>(null);
  const config = pasarela.datos;

  useEffect(() => {
    if (config && !formulario) setFormulario(config);
  }, [config, formulario]);

  async function guardar(nueva: ConfigPasarela) {
    setError(null);
    try {
      const guardada = await api.configurarPasarela(nueva);
      setFormulario(guardada);
      await pasarela.recargar();
    } catch (e) {
      setError(mensajeError(e));
    }
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (formulario) void guardar(formulario);
  }

  return (
    <section className="tarjeta">
      <div className="titulo-con-estado">
        <h2>Pasarela de pagos (simulada)</h2>
        {config && <span className={`etiqueta ${config.caida ? 'tono-error' : 'tono-exito'}`}>{config.caida ? 'Caída' : 'Operativa'}</span>}
      </div>
      <p className="suave">
        Si la pasarela cae, los pedidos se siguen registrando en estado «Confirmando pago» y los cobros se reintentan
        solos con espera creciente hasta que vuelva.
      </p>
      <EstadoCarga cargando={pasarela.cargando} error={pasarela.error} />
      {error && <div className="aviso aviso-error">{error}</div>}
      {config && (
        <button className={`boton boton-ancho ${config.caida ? '' : 'boton-peligro'}`}
                onClick={() => guardar({ ...config, caida: !config.caida })}>
          {config.caida ? 'Restablecer la pasarela' : 'Simular caída de la pasarela'}
        </button>
      )}
      {formulario && (
        <form className="formulario-rejilla" onSubmit={enviar}>
          <label className="campo">
            Fallos (0–1)
            <input type="number" step="0.05" min={0} max={1} value={formulario.tasaFallo}
                   onChange={(e) => setFormulario({ ...formulario, tasaFallo: Number(e.target.value) })} />
          </label>
          <label className="campo">
            Rechazos (0–1)
            <input type="number" step="0.05" min={0} max={1} value={formulario.tasaRechazo}
                   onChange={(e) => setFormulario({ ...formulario, tasaRechazo: Number(e.target.value) })} />
          </label>
          <label className="campo">
            Latencia (ms)
            <input type="number" step="100" min={0} value={formulario.latenciaMs}
                   onChange={(e) => setFormulario({ ...formulario, latenciaMs: Number(e.target.value) })} />
          </label>
          <button className="boton boton-secundario">Aplicar</button>
        </form>
      )}
    </section>
  );
}

function BuscarPago() {
  const [pedidoId, setPedidoId] = useState('');
  const [pago, setPago] = useState<Pago | null | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);

  async function buscar(evento: FormEvent) {
    evento.preventDefault();
    setError(null);
    try {
      setPago(await api.pagoDePedido(Number(pedidoId)));
    } catch (e) {
      setError(mensajeError(e));
    }
  }

  return (
    <section className="tarjeta">
      <h2>Soporte: buscar pago</h2>
      <form className="formulario-linea" onSubmit={buscar}>
        <input type="number" min={1} placeholder="Número de pedido" required value={pedidoId} onChange={(e) => setPedidoId(e.target.value)} />
        <button className="boton">Buscar</button>
      </form>
      {error && <div className="aviso aviso-error">{error}</div>}
      {pago === null && <p className="suave">Ese pedido no tiene pago registrado.</p>}
      {pago && (
        <dl className="datos">
          <dt>Estado</dt><dd><span className={`etiqueta ${pago.estado === 'APROBADO' ? 'tono-exito' : pago.estado === 'PENDIENTE' ? 'tono-espera' : 'tono-error'}`}>{pago.estado.toLowerCase()}</span></dd>
          <dt>Método</dt><dd>{pago.metodo}</dd>
          <dt>Monto</dt><dd>{dinero(pago.monto)}</dd>
          <dt>Intentos</dt><dd>{pago.intentos}</dd>
          {pago.referencia && <><dt>Referencia</dt><dd>{pago.referencia}</dd></>}
          {pago.motivo && <><dt>Motivo</dt><dd>{pago.motivo}</dd></>}
          <dt>Actualizado</dt><dd>{fechaHora(pago.actualizadoEn)}</dd>
        </dl>
      )}
    </section>
  );
}
