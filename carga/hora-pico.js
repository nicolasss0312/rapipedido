// Prueba de carga del escenario de RENDIMIENTO:
// "3.000 clientes confirman y pagan en hora de almuerzo; < 2 s para el 95 % de los pedidos".
// 3.000 pedidos/hora ≈ 0,83 por segundo. Por defecto probamos 10 pedidos/s (12 veces más) durante 2 min.
//
// Ejecutar (con el sistema arriba):
//   docker run --rm -i --network rapipedido_default -e BASE_URL=http://web grafana/k6 run - < carga/hora-pico.js
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const TASA = parseInt(__ENV.TASA || '10');
const METODOS = ['TARJETA', 'PSE', 'EFECTIVO'];

export const options = {
  scenarios: {
    hora_pico: {
      executor: 'constant-arrival-rate',
      rate: TASA,
      timeUnit: '1s',
      duration: __ENV.DURACION || '2m',
      preAllocatedVUs: 20,
      maxVUs: 200,
    },
  },
  thresholds: {
    'http_req_duration{tipo:crear_pedido}': ['p(95)<2000'],   // la medida del PDF
    'http_req_duration{tipo:menu}': ['p(95)<300'],            // catálogo servido desde Redis
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const restauranteId = 1 + Math.floor(Math.random() * 3);
  const menu = http.get(`${BASE}/api/restaurantes/${restauranteId}/menu`, { tags: { tipo: 'menu' } });
  const productos = menu.json();
  const producto = productos[Math.floor(Math.random() * productos.length)];

  const pedido = {
    clienteId: 1 + Math.floor(Math.random() * 5000),
    restauranteId,
    metodoPago: METODOS[Math.floor(Math.random() * METODOS.length)],
    direccionEntrega: 'Calle 100 # 15-20, Bogotá',
    items: [{ productoId: producto.id, cantidad: 1 + Math.floor(Math.random() * 3) }],
  };
  const res = http.post(`${BASE}/api/pedidos`, JSON.stringify(pedido), {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': `k6-${__VU}-${__ITER}-${Date.now()}` },
    tags: { tipo: 'crear_pedido' },
  });
  check(res, { 'pedido registrado (202)': (r) => r.status === 202 });
}
