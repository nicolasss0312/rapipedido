/** Muestra "cargando" o el error de una consulta. */
export function EstadoCarga({ cargando, error }: { cargando: boolean; error: string | null }) {
  if (error) return <div className="aviso aviso-error">{error}</div>;
  if (cargando) return <div className="cargando">Cargando…</div>;
  return null;
}
