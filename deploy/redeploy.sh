#!/usr/bin/env sh
set -eu

DEPLOY_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.release.yml"
ENV_FILE="$DEPLOY_DIR/.env"
IMAGE_TAG="${1:-${IMAGE_TAG:-latest}}"
GHCR_USERNAME="${GHCR_USERNAME:-}"
GHCR_TOKEN="${GHCR_TOKEN:-}"

export OFFERPILOT_BACKEND_IMAGE="${OFFERPILOT_BACKEND_IMAGE:-ghcr.io/sreehc/offerpilot-backend:${IMAGE_TAG}}"
export OFFERPILOT_FRONTEND_IMAGE="${OFFERPILOT_FRONTEND_IMAGE:-ghcr.io/sreehc/offerpilot-frontend:${IMAGE_TAG}}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

if [ -n "$GHCR_USERNAME" ] && [ -n "$GHCR_TOKEN" ]; then
  printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull backend frontend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d backend frontend redis --remove-orphans
docker image prune -f >/dev/null 2>&1 || true

echo "Redeployed OfferPilot with tag: $IMAGE_TAG"
