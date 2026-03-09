#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Starting local PostgreSQL container..."
docker compose -f docker-compose.yml up -d postgres

echo "Running OmniSolve API with local profile..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

