#!/usr/bin/env bash
# Carga pedidos de prueba y los reparte entre las columnas del tablero del restaurante.
#
#   ./scripts/cargar-pedidos.sh                 # 12 pedidos en el restaurante 1
#   ./scripts/cargar-pedidos.sh 20 2            # 20 pedidos en el restaurante 2
#   ./scripts/cargar-pedidos.sh 30 todos        # 30 pedidos repartidos entre todos los restaurantes
#   ESTADOS=nuevos ./scripts/cargar-pedidos.sh  # todos se quedan en "Nuevos" (sin avanzar)
set -euo pipefail
API=${API:-http://localhost:8080/api}
CANTIDAD=${1:-12}
RESTAURANTE=${2:-1}
ESTADOS=${ESTADOS:-variados}
METODOS=(TARJETA PSE EFECTIVO)
DIRECCIONES=("Calle 93 # 11-30" "Cra 7 # 72-10" "Calle 85 # 15-22" "Av. 19 # 100-45" "Cra 11 # 82-71" "Calle 67 # 9-30")

command -v jq >/dev/null || { echo "Se necesita jq (brew install jq)"; exit 1; }
curl -sf "$API/restaurantes" >/dev/null || { echo "La API no responde en $API. ¿Está corriendo docker compose?"; exit 1; }

if [ "$RESTAURANTE" = todos ]; then
  RESTAURANTES=($(curl -s "$API/restaurantes" | jq -r '.[] | select(.abierto) | .id'))
else
  RESTAURANTES=("$RESTAURANTE")
fi

echo "Creando $CANTIDAD pedidos…"
IDS=()
for i in $(seq 1 "$CANTIDAD"); do
  rid=${RESTAURANTES[$((RANDOM % ${#RESTAURANTES[@]}))]}
  # 1 a 3 productos disponibles del menú, con cantidades de 1 a 3
  items=$(curl -s "$API/restaurantes/$rid/menu" | jq -c --argjson n $((1 + RANDOM % 3)) --argjson s $RANDOM '
    [ .[] | select(.disponible) ] as $m
    | [ range(0; $n) | $m[(($s + . * 7) % ($m | length))] ] | unique_by(.id)
    | map({productoId: .id, cantidad: (1 + (($s + .id) % 3))})')
  cuerpo=$(jq -nc --argjson c $((100 + RANDOM % 900)) --argjson r "$rid" --arg m "${METODOS[$((RANDOM % 3))]}" \
    --arg d "${DIRECCIONES[$((RANDOM % ${#DIRECCIONES[@]}))]}, Bogotá" --argjson it "$items" \
    '{clienteId: $c, restauranteId: $r, metodoPago: $m, direccionEntrega: $d, items: $it}')
  id=$(curl -s -X POST "$API/pedidos" -H 'Content-Type: application/json' -H "Idempotency-Key: carga-$(date +%s)-$i-$RANDOM" -d "$cuerpo" | jq -r '.id // empty')
  if [ -n "$id" ]; then IDS+=("$id"); printf '  pedido #%s (restaurante %s)\n' "$id" "$rid"; else echo "  ✗ no se pudo crear un pedido"; fi
done

echo "Esperando a que se procesen los pagos…"
for _ in $(seq 1 30); do
  pendientes=0
  for id in "${IDS[@]}"; do [ "$(curl -s "$API/pedidos/$id" | jq -r .estado)" = PAGO_PENDIENTE ] && pendientes=$((pendientes + 1)); done
  [ "$pendientes" = 0 ] && break
  sleep 1
done

avanzar() { # pedido estado...
  local id=$1; shift
  for e in "$@"; do
    curl -s -o /dev/null -X PATCH "$API/pedidos/$id/estado" -H 'Content-Type: application/json' -d "{\"estado\":\"$e\"}"
  done
}

if [ "$ESTADOS" = variados ]; then
  echo "Repartiendo los pedidos en el tablero…"
  n=0
  for id in "${IDS[@]}"; do
    [ "$(curl -s "$API/pedidos/$id" | jq -r .estado)" = PAGADO ] || continue
    case $((n % 4)) in
      0) ;;                                              # se queda en Nuevos
      1) avanzar "$id" ACEPTADO ;;
      2) avanzar "$id" ACEPTADO EN_PREPARACION ;;
      3) avanzar "$id" ACEPTADO EN_PREPARACION LISTO ;;  # pasa a Despacho y se le busca repartidor
    esac
    n=$((n + 1))
  done
  sleep 2
fi

echo
echo "Resumen de pedidos por estado:"
curl -s "$API/pedidos/resumen" | jq -r 'to_entries[] | select(.value > 0) | "  \(.key): \(.value)"'
