import { useCallback, useEffect, useRef, useState } from 'react';
import { mensajeError } from './api';

/**
 * Carga datos de la API y, si se indica un intervalo, los vuelve a pedir periódicamente
 * (así los paneles ven los cambios que llegan por eventos sin recargar la página).
 */
export function useConsulta<T>(cargar: () => Promise<T>, deps: unknown[], intervaloMs?: number) {
  const [datos, setDatos] = useState<T | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);
  const cargarRef = useRef(cargar);
  cargarRef.current = cargar;
  const generacion = useRef(0);

  const recargar = useCallback(async () => {
    const mia = generacion.current;
    try {
      const resultado = await cargarRef.current();
      if (mia === generacion.current) {
        setDatos(resultado);
        setError(null);
      }
    } catch (e) {
      if (mia === generacion.current) setError(mensajeError(e));
    } finally {
      if (mia === generacion.current) setCargando(false);
    }
  }, []);

  useEffect(() => {
    generacion.current++;
    setDatos(undefined);
    setCargando(true);
    void recargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    if (!intervaloMs) return;
    const temporizador = setInterval(() => void recargar(), intervaloMs);
    return () => clearInterval(temporizador);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [intervaloMs, recargar, ...deps]);

  return { datos, error, cargando, recargar };
}

/** Preferencia del usuario (cliente, restaurante o repartidor elegido) guardada en el navegador. */
export function usePreferencia<T>(clave: string, inicial: T): [T, (valor: T) => void] {
  const nombre = 'rapipedido.' + clave;
  const [valor, setValor] = useState<T>(() => {
    try {
      const guardado = localStorage.getItem(nombre);
      return guardado !== null ? (JSON.parse(guardado) as T) : inicial;
    } catch {
      return inicial;
    }
  });
  const guardar = useCallback((nuevo: T) => {
    setValor(nuevo);
    try {
      localStorage.setItem(nombre, JSON.stringify(nuevo));
    } catch {
      // almacenamiento no disponible (modo privado): solo se pierde la preferencia
    }
  }, [nombre]);
  return [valor, guardar];
}
