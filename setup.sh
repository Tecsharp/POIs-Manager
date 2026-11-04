#!/usr/bin/env bash
# Levanta todo POIs Manager: genera .env (solo la primera vez), construye y arranca los contenedores.
set -euo pipefail
cd "$(dirname "$0")"

say()  { printf '\033[1;36m▸ %s\033[0m\n' "$*"; }
fail() { printf '\033[1;31m✖ %s\033[0m\n' "$*" >&2; exit 1; }
rand() { LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom 2>/dev/null | head -c "$1" || true; }

command -v docker >/dev/null || fail "Docker no está instalado: https://docs.docker.com/get-docker/"
docker info >/dev/null 2>&1   || fail "Docker no está corriendo. Inícialo y vuelve a ejecutar ./setup.sh"
docker compose version >/dev/null 2>&1 || fail "Necesitas Docker Compose v2 (comando: docker compose)"

if [ ! -f .env ]; then
  say "Generando configuración y credenciales (.env)"
  port=8080
  # Si el puerto está ocupado, usa el siguiente libre.
  while (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; do port=$((port + 1)); done
  umask 077
  cat > .env <<ENV
# Generado por setup.sh el $(date -u +%Y-%m-%dT%H:%M:%SZ). No lo subas al repositorio.
APP_PORT=$port
POSTGRES_DB=pois
POSTGRES_USER=pois
POSTGRES_PASSWORD=$(rand 32)
JWT_SECRET=$(rand 64)
ADMIN_EMAIL=admin@pois.local
ADMIN_PASSWORD=$(rand 16)
ANON_REQUESTS_PER_MINUTE=60
API_KEY_DAILY_LIMIT=1000
API_KEY_MAX_PER_USER=3
ENV
else
  say "Usando .env existente"
fi

set -a
# shellcheck disable=SC1091
. ./.env
set +a

say "Construyendo e iniciando contenedores (la primera vez tarda unos minutos)…"
if ! docker compose up -d --build --wait --wait-timeout 600; then
  docker compose ps
  docker compose logs --tail 80 backend
  fail "Algún contenedor no arrancó. Revisa los logs de arriba."
fi

URL="http://localhost:${APP_PORT}"
cat <<MSG

  ┌──────────────────────────────────────────────────────────┐
     POIs Manager está listo

     Web:            $URL
     API:            $URL/api
     Docs de la API: $URL/docs

     Administrador
       Usuario:      $ADMIN_EMAIL
       Contraseña:   $ADMIN_PASSWORD
  └──────────────────────────────────────────────────────────┘

  Ver logs:   docker compose logs -f
  Detener:    docker compose down
  Reiniciar desde cero: docker compose down -v, borra el archivo .env y vuelve a correr ./setup.sh

MSG
