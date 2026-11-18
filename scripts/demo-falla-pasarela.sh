#!/usr/bin/env bash
# Escenario de DISPONIBILIDAD: "La pasarela no responde en hora pico; el sistema guarda el pago
# pendiente, reintenta y sigue recibiendo pedidos".
set -euo pipefail
API=${API:-http://localhost:8080/api}
paso() { printf '\n\033[1;32m▶ %s\033[0m\n' "$1"; }
crear() {
  curl -s -o /tmp/rapipedido-resp.json -w '%{http_code} %{time_total}s' -X POST "$API/pedidos" \
    -H 'Content-Type: application/json' \
    -d "{\"clienteId\":$1,\"restauranteId\":2,\"metodoPago\":\"PSE\",\"direccionEntrega\":\"Cra 7 # 72-10\",\"items\":[{\"productoId\":4,\"cantidad\":1}]}"
}

paso "Tumbamos la pasarela de pagos"
curl -s -X PUT "$API/admin/pasarela" -H 'Content-Type: application/json' \
  -d '{"caida":true,"tasaFallo":0,"tasaRechazo":0,"latenciaMs":200}' | jq -c

paso "Los clientes siguen pidiendo: el sistema responde igual de rápido"
IDS=()
for c in 101 102 103; do
  r=$(crear $c); IDS+=("$(jq -r .id /tmp/rapipedido-resp.json)")
  echo "  HTTP/tiempo: $r → pedido $(jq -r '.id' /tmp/rapipedido-resp.json) en $(jq -r '.estado' /tmp/rapipedido-resp.json)"
done

paso "Mientras tanto, los pagos se reintentan con espera creciente"
sleep 5
for id in "${IDS[@]}"; do
  echo "  pedido $id: $(curl -s "$API/pedidos/$id" | jq -r .estado) | pago: $(curl -s "$API/pagos/pedido/$id" | jq -c '{estado, intentos}')"
done

paso "La pasarela vuelve"
curl -s -X PUT "$API/admin/pasarela" -H 'Content-Type: application/json' \
  -d '{"caida":false,"tasaFallo":0,"tasaRechazo":0,"latenciaMs":300}' | jq -c

paso "Ningún pedido se perdió: todos terminan pagados"
for _ in $(seq 1 40); do
  pend=0; for id in "${IDS[@]}"; do [ "$(curl -s "$API/pedidos/$id" | jq -r .estado)" = PAGO_PENDIENTE ] && pend=1; done
  [ $pend = 0 ] && break; sleep 1
done
for id in "${IDS[@]}"; do
  echo "  pedido $id: $(curl -s "$API/pedidos/$id" | jq -r .estado) | pago: $(curl -s "$API/pagos/pedido/$id" | jq -c '{estado, intentos}')"
done
