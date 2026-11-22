#!/usr/bin/env bash
# Prueba de punta a punta contra la API levantada (local o en CI).
# Uso: ./scripts/prueba-humo.sh [url-base]
set -euo pipefail

BASE="${1:-http://localhost:8080/api/tareas}"
FALLAS=0

esperar_api() {
    echo "Esperando a que la API responda en $BASE ..."
    for _ in $(seq 1 60); do
        if curl -fs "$BASE" > /dev/null; then echo "Lista."; return 0; fi
        sleep 3
    done
    echo "La API no respondió a tiempo"; exit 1
}

# chequear "descripción" código-esperado método url [json]
chequear() {
    local desc="$1" esperado="$2" metodo="$3" url="$4" cuerpo="${5:-}"
    local args=(-s -o /tmp/respuesta.json -w '%{http_code}' -X "$metodo" "$url")
    [[ -n "$cuerpo" ]] && args+=(-H 'Content-Type: application/json' -d "$cuerpo")
    local obtenido
    obtenido=$(curl "${args[@]}")
    if [[ "$obtenido" == "$esperado" ]]; then
        echo "  OK   $desc ($obtenido)"
    else
        echo "  MAL  $desc: esperaba $esperado, vino $obtenido"
        cat /tmp/respuesta.json; echo
        FALLAS=$((FALLAS + 1))
    fi
}

esperar_api

chequear "crear tarea"                    201 POST  "$BASE" '{"titulo":"Probar la API","prioridad":"ALTA"}'
ID=$(grep -o '"id":[0-9]*' /tmp/respuesta.json | head -1 | cut -d: -f2)

chequear "obtener tarea creada"           200 GET   "$BASE/$ID"
chequear "listar filtrando por estado"    200 GET   "$BASE?estado=PENDIENTE"
chequear "título vacío es rechazado"      400 POST  "$BASE" '{"titulo":""}'
chequear "no se puede terminar sin empezar" 409 PATCH "$BASE/$ID/estado" '{"estado":"HECHA"}'
chequear "empezar tarea"                  200 PATCH "$BASE/$ID/estado" '{"estado":"EN_CURSO"}'
chequear "terminar tarea"                 200 PATCH "$BASE/$ID/estado" '{"estado":"HECHA"}'
chequear "actualizar tarea"               200 PUT   "$BASE/$ID" '{"titulo":"Probar la API (editada)"}'
chequear "eliminar tarea"                 204 DELETE "$BASE/$ID"
chequear "tarea eliminada da 404"         404 GET   "$BASE/$ID"

echo
if (( FALLAS > 0 )); then echo "$FALLAS prueba(s) fallaron"; exit 1; fi
echo "Todas las pruebas pasaron"
