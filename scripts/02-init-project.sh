#!/usr/bin/env bash
#
# Inicializa o VaultDesk do zero: gera o .env (se nao existir), sobe a stack
# completa via Docker Compose e espera backend/frontend ficarem saudaveis.
#
# Pre-requisito: Docker (Desktop, com integracao WSL2 se for Windows) ja
# instalado e rodando. Nao instala nada -- so orquestra o que ja existe.
#
# Uso: ./scripts/02-init-project.sh

set -euo pipefail

log() { printf '\n\033[1;36m==> %s\033[0m\n' "$1"; }
warn() { printf '\033[1;33m! %s\033[0m\n' "$1"; }
err() { printf '\033[1;31mERRO: %s\033[0m\n' "$1" >&2; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

BACKEND_URL="http://localhost:8000"
FRONTEND_URL="http://localhost:3000"

if ! command -v docker >/dev/null 2>&1; then
    err "docker nao encontrado no PATH. Instale o Docker Desktop antes de rodar este script."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    err "Docker esta instalado mas o daemon nao responde. Abra o Docker Desktop e tente de novo."
    exit 1
fi

if [ ! -f .env ]; then
    log "Nenhum .env encontrado -- gerando um novo com segredos aleatorios"
    if ! command -v openssl >/dev/null 2>&1; then
        err "openssl nao encontrado; nao consigo gerar os segredos automaticamente."
        err "Copie .env.example para .env e preencha os valores manualmente."
        exit 1
    fi
    {
        echo "POSTGRES_PASSWORD=$(openssl rand -base64 24 | tr -d '\n')"
        echo "VAULTDESK_JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')"
        echo "VAULTDESK_MASTER_KEY=$(openssl rand -base64 32 | tr -d '\n')"
    } > .env
    echo ".env criado."
else
    log ".env ja existe -- reaproveitando os segredos atuais"
fi

log "Subindo a stack (docker compose up -d --build)"
docker compose up -d --build

wait_for() {
    local name="$1" url="$2" timeout="${3:-90}"
    log "Esperando $name responder em $url (timeout ${timeout}s)"
    local waited=0
    until curl -sf -o /dev/null "$url"; do
        if [ "$waited" -ge "$timeout" ]; then
            warn "$name nao respondeu em ${timeout}s. Confira os logs: docker compose logs $name"
            return 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
    echo "$name esta de pe (levou ${waited}s)."
}

wait_for "backend" "$BACKEND_URL/actuator/health" 90 || true
wait_for "frontend" "$FRONTEND_URL" 60 || true

log "Status dos containers"
docker compose ps

cat <<EOF

VaultDesk pronto:
  Frontend: $FRONTEND_URL
  Backend:  $BACKEND_URL/api/v1
  Health:   $BACKEND_URL/actuator/health

Sem conta pre-criada -- abra o frontend e clique em "Cadastre-se".
EOF
