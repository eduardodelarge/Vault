#!/usr/bin/env bash
#
# Apaga TODOS os containers, imagens, volumes, redes e cache de build do Docker
# desta maquina (nao so os do VaultDesk) e, se estiver rodando dentro do WSL2,
# tenta compactar o disco virtual do Docker Desktop (docker_data.vhdx) para
# devolver o espaco de verdade ao host Windows.
#
# ATENCAO: isso e destrutivo e ABRANGE QUALQUER OUTRO PROJETO que use Docker
# nesta maquina, nao so o VaultDesk. Os dados do SaaS (usuarios/notas) tambem
# sao perdidos -- rode 02-init-project.sh depois para reconstruir o projeto
# do zero (schema vazio).
#
# Uso: ./scripts/01-clean-docker.sh

set -euo pipefail

log() { printf '\n\033[1;36m==> %s\033[0m\n' "$1"; }
warn() { printf '\033[1;33m! %s\033[0m\n' "$1"; }

if ! command -v docker >/dev/null 2>&1; then
    echo "docker nao encontrado no PATH. Nada a fazer." >&2
    exit 1
fi

log "Uso de disco do Docker ANTES da limpeza"
docker system df || true

log "Descendo a stack do VaultDesk (se estiver rodando)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
(cd "$PROJECT_DIR" && docker compose down --rmi all -v --remove-orphans) || true

log "Parando todos os containers da maquina (de qualquer projeto)"
docker ps -q | xargs -r docker stop

log "Removendo todos os containers"
docker ps -aq | xargs -r docker rm -f

log "Removendo todas as imagens"
docker images -aq | xargs -r docker rmi -f

log "Removendo todos os volumes"
docker volume ls -q | xargs -r docker volume rm -f

log "Removendo redes customizadas (as 3 redes padrao do Docker nao sao removiveis, isso e normal)"
docker network ls --filter type=custom -q | xargs -r docker network rm

log "Limpando todo o cache de build"
docker builder prune -a -f

log "Varredura final (system prune)"
docker system prune -a --volumes -f

log "Verificando se sobrou algum rastro"
CONTAINERS_LEFT=$(docker ps -aq | wc -l | tr -d ' ')
IMAGES_LEFT=$(docker images -aq | wc -l | tr -d ' ')
VOLUMES_LEFT=$(docker volume ls -q | wc -l | tr -d ' ')

echo "Containers restantes: $CONTAINERS_LEFT"
echo "Imagens restantes:    $IMAGES_LEFT"
echo "Volumes restantes:    $VOLUMES_LEFT"

if [ "$CONTAINERS_LEFT" -eq 0 ] && [ "$IMAGES_LEFT" -eq 0 ] && [ "$VOLUMES_LEFT" -eq 0 ]; then
    echo "OK: nenhum rastro de container, imagem ou volume ficou para tras."
else
    warn "Ainda sobrou algo. Rode 'docker ps -a', 'docker images -a' e 'docker volume ls' para investigar."
fi

log "Uso de disco do Docker DEPOIS da limpeza"
docker system df || true

# --- Compactacao do disco virtual (so faz sentido dentro do WSL2) ---
if grep -qi microsoft /proc/version 2>/dev/null && command -v powershell.exe >/dev/null 2>&1; then
    log "Detectado WSL2: tentando compactar o docker_data.vhdx no Windows"
    warn "Isso vai desligar TODAS as distros WSL (inclusive esta sessao) por alguns segundos -- e esperado."
    warn "Uma janela do PowerShell vai pedir permissao de administrador (UAC); aceite para a compactacao rodar."

    powershell.exe -NoProfile -Command '
      Start-Process powershell -Verb RunAs -ArgumentList @(
        "-NoProfile","-Command",
        "Start-Sleep -Seconds 3; wsl --shutdown; Start-Sleep -Seconds 5; Import-Module Hyper-V -ErrorAction SilentlyContinue; if (Get-Command Optimize-VHD -ErrorAction SilentlyContinue) { Optimize-VHD -Path \"$env:LOCALAPPDATA\Docker\wsl\disk\docker_data.vhdx\" -Mode Full; Write-Host \"VHDX compactado com sucesso.\" } else { Write-Host \"Modulo Hyper-V/Optimize-VHD indisponivel. Rode manualmente: Optimize-VHD -Path $env:LOCALAPPDATA\Docker\wsl\disk\docker_data.vhdx -Mode Full\" }; Start-Sleep -Seconds 15"
      )
    ' || warn "Nao consegui disparar o PowerShell elevado. Rode manualmente (PowerShell administrador): wsl --shutdown; Optimize-VHD -Path \"\$env:LOCALAPPDATA\Docker\wsl\disk\docker_data.vhdx\" -Mode Full"

    log "Compactacao disparada em uma janela separada. Esta sessao WSL pode cair em alguns segundos -- isso e esperado."
else
    log "Nao estou num WSL2 com acesso a powershell.exe (ou nao e Windows) -- pulando a compactacao do disco virtual."
fi

log "Limpeza concluida."
