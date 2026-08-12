# VaultDesk — Simple Credential & Notes Manager

Pequeno SaaS local para gerenciar notas/credenciais com conteúdo criptografado em repouso (AES-GCM). Rodando 100% localmente via Docker Compose; preparado para receber instrumentação do Datadog (APM + logs) numa fase futura.

## Stack

- **Backend**: Java 21 + Spring Boot 4.1 (Maven), PostgreSQL, Flyway, Spring Security + JWT.
- **Frontend**: React + Vite + TypeScript (SPA).
- **Infra local**: Docker Compose (`postgres`, `backend`, `frontend`).

## Pré-requisitos

- Docker Desktop (com integração WSL2 habilitada, se estiver no WSL) — `docker` e `docker compose` precisam funcionar no shell.
- Para desenvolvimento fora de containers: Java 21 + Maven (backend) e Node 20+ (frontend). Este ambiente já tem um toolchain local instalado em `~/.local/opt/` (ver `~/.bashrc`), sem precisar de instalação via sistema.

## Configuração

```bash
cp .env.example .env
# gerar segredos:
# POSTGRES_PASSWORD, VAULTDESK_JWT_SECRET, VAULTDESK_MASTER_KEY (Base64 de 32 bytes)
openssl rand -base64 32
```

`.env` nunca deve ser commitado (já está no `.gitignore`).

## Rodando localmente

**Stack completa (produção-like):**

```bash
docker compose up --build
# backend:  http://localhost:8000
# frontend: http://localhost:3000
```

> Nota: o backend expõe a porta **8000** no host (não 8080) porque, rodando via WSL2, o Windows/Hyper-V costuma reservar dinamicamente a faixa 8071-8170 para si (`netsh interface ipv4 show excludedportrange protocol=tcp`), o que impede o Docker Desktop de publicar a porta 8080. Dentro da rede Docker o container continua ouvindo em 8080 normalmente.

**Dev do frontend com hot-reload** (recomendado durante desenvolvimento):

```bash
docker compose up -d postgres backend
cd frontend && npm install && npm run dev
# frontend dev server: http://localhost:5173, consumindo a API em localhost:8000
```

## Verificação rápida

```bash
curl -s localhost:8000/actuator/health | jq
```

## Roteiro de implementação

O projeto foi construído em fases incrementais (ver plano completo em `/home/cdx/.claude/plans/dynamic-coalescing-cherny.md`), todas concluídas:

1. Setup do projeto e infraestrutura ✅
2. Autenticação (JWT) ✅
3. CRUD de notas + criptografia AES-GCM ✅
4. Search notes ✅
5. Account info + change password ✅
6. Preparação para observabilidade/Datadog ✅

## Observabilidade

Já preparado para plugar o Datadog (ou qualquer stack compatível com Prometheus/JSON logs) sem mudar código:

- **Logs**: JSON estruturado no stdout (`logback-spring.xml` + `logstash-logback-encoder`), incluindo o campo `traceId` — o mesmo valor devolvido no header `X-Request-Id` da resposta e no corpo de erros da API (`ErrorResponse.traceId`), permitindo correlacionar um erro visto pelo usuário com a linha de log exata.
- **Métricas**: `/actuator/prometheus` (formato Prometheus) e `/actuator/metrics` expostos publicamente (sem dado de usuário, só métricas de infraestrutura) — prontos para scraping pelo Datadog Agent ou por um Prometheus local.
- **Nomes de containers já organizados**: `vaultdesk-backend`, `vaultdesk-postgres`, `vaultdesk-frontend`.

**Para plugar o Datadog Agent depois:**

1. Baixe o [Datadog Java APM Agent](https://docs.datadoghq.com/tracing/trace_collection/dd_libraries/java/) (`dd-java-agent.jar`) para `backend/` (ou monte como volume no `docker-compose.yml`).
2. Adicione no `.env`: `DD_API_KEY`, `DD_ENV=local`, `DD_SERVICE=vaultdesk-backend`, `DD_VERSION=0.0.1`.
3. No `docker-compose.yml`, descomente a linha `JAVA_TOOL_OPTIONS: "-javaagent:/dd-java-agent.jar"` do serviço `backend`.
4. Suba um container do Datadog Agent no mesmo `docker-compose.yml` (ou use o Agent já rodando no host) e aponte `DD_AGENT_HOST` para ele.

## Notas de segurança

- Conteúdo das notas é cifrado com AES-256-GCM antes de persistir; `title` fica em texto plano para permitir busca indexada.
- Senhas de usuário usam BCrypt (custo 12).
- Refresh tokens são armazenados como hash SHA-256, nunca em texto puro; rotacionados a cada uso.
- Se o ciphertext de uma nota for adulterado no banco, a auth tag do AES-GCM falha e a API responde 500 (fail closed) — a nota fica ilegível/indeletável via API até uma correção manual direta no banco, por design.
