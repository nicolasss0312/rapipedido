#!/usr/bin/env bash
# Recorre el flujo completo de un pedido: cliente → pago → restaurante → repartidor → entrega.
set -euo pipefail
API=${API:-http://localhost:8080/api}
paso() { printf '\n\033[1;32m▶ %s\033[0m\n' "$1"; }
estado() { curl -s "$API/pedidos/$1" | jq -r '.estado'; }
esperar_estado() { # pedido estado
  for _ in $(seq 1 30); do [ "$(estado "$1")" = "$2" ] && { echo "  estado: $2"; return; }; sleep 1; done
  echo "  ✗ no llegó a $2 (está en $(estado "$1"))"; exit 1
}

paso "Menú del restaurante 1 (desde caché Redis)"
curl -s "$API/restaurantes/1/menu" | jq -c '.[] | {id, nombre, precio}'

paso "El cliente 42 confirma su carrito y paga con tarjeta"
PEDIDO=$(curl -s -X POST "$API/pedidos" -H 'Content-Type: application/json' \
  -H "Idempotency-Key: demo-$(date +%s)" \
  -d '{"clienteId":42,"restauranteId":1,"metodoPago":"TARJETA","direccionEntrega":"Calle 93 # 11-30",
       "items":[{"productoId":1,"cantidad":1},{"productoId":3,"cantidad":2}]}')
echo "$PEDIDO" | jq -c '{id, estado, total}'
ID=$(echo "$PEDIDO" | jq -r '.id')

paso "El pago se procesa de forma asíncrona"
esperar_estado "$ID" PAGADO
curl -s "$API/pagos/pedido/$ID" | jq -c '{estado, intentos, referencia}'

paso "El restaurante acepta, prepara y marca listo"
for e in ACEPTADO EN_PREPARACION LISTO; do
  curl -s -X PATCH "$API/pedidos/$ID/estado" -H 'Content-Type: application/json' -d "{\"estado\":\"$e\"}" | jq -r '"  estado: " + .estado'
done

paso "Se asigna el repartidor más cercano"
esperar_estado "$ID" ASIGNADO
REP=$(curl -s "$API/pedidos/$ID" | jq -r '.repartidorId')
echo "  repartidor: $REP"

paso "El repartidor recoge y envía su ubicación (normalmente cada 5 s por WebSocket)"
curl -s -X POST "$API/entregas/$ID/recoger" | jq -c '{estado}'
for p in "4.6766 -74.0485" "4.6740 -74.0500" "4.6712 -74.0521"; do
  set -- $p
  curl -s -X POST "$API/tracking/ubicacion" -H 'Content-Type: application/json' \
    -d "{\"repartidorId\":$REP,\"pedidoId\":$ID,\"lat\":$1,\"lng\":$2}" | jq -c '{lat, lng}'
done
esperar_estado "$ID" EN_CAMINO

paso "Entrega confirmada"
curl -s -X POST "$API/entregas/$ID/confirmar" | jq -c '{estado}'
esperar_estado "$ID" ENTREGADO
echo; echo "Revisa las notificaciones simuladas con: docker compose logs app | grep -E 'PUSH|SMS'"
