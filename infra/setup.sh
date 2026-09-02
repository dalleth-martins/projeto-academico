#!/bin/bash
set -e

echo "Gerando mongo-keyfile..."

if [ -f mongo-keyfile ]; then
  echo "mongo-keyfile já existe, pulando geração."
else
  MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd):/out" busybox sh -c "head -c 756 /dev/urandom | base64 | tr -d '\n' > /out/mongo-keyfile && echo >> /out/mongo-keyfile"
  MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd)/mongo-keyfile:/keyfile" mongo:7 bash -c "chown mongodb:mongodb /keyfile && chmod 400 /keyfile"
  echo "mongo-keyfile criado com sucesso."
fi

if [ ! -f .env ]; then
  cp .env.example .env
  echo ".env criado a partir do .env.example — ajuste as senhas se quiser."
fi

echo "Setup completo. Rode: docker-compose up -d"