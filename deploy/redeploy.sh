#!/usr/bin/env sh
set -eu

DEPLOY_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.release.yml"
ENV_FILE="$DEPLOY_DIR/.env"
IMAGE_TAG="${1:-${IMAGE_TAG:-latest}}"
GHCR_USERNAME="${GHCR_USERNAME:-}"
GHCR_TOKEN="${GHCR_TOKEN:-}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-offerpilot}"
APP_ROOT="${APP_ROOT:-/srv/offerpilot}"
FRONTEND_RELEASE_DIR="${FRONTEND_RELEASE_DIR:-$APP_ROOT/frontend/releases/$IMAGE_TAG}"
FRONTEND_CURRENT_LINK="${FRONTEND_CURRENT_LINK:-$APP_ROOT/frontend/current}"
FRONTEND_ARTIFACT="${FRONTEND_ARTIFACT:-$DEPLOY_DIR/frontend-dist.tgz}"
NGINX_CONF_SOURCE="${NGINX_CONF_SOURCE:-$DEPLOY_DIR/nginx/offerpilot.conf}"
NGINX_CONF_TARGET="${NGINX_CONF_TARGET:-/www/server/panel/vhost/nginx/offerpilot.conf}"

export OFFERPILOT_BACKEND_IMAGE="${OFFERPILOT_BACKEND_IMAGE:-ghcr.io/sreehc/offerpilot-backend:${IMAGE_TAG}}"
export COMPOSE_PROJECT_NAME

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

if [ -n "$GHCR_USERNAME" ] && [ -n "$GHCR_TOKEN" ]; then
  printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
fi

if [ -f "$FRONTEND_ARTIFACT" ]; then
  rm -rf "$FRONTEND_RELEASE_DIR"
  mkdir -p "$FRONTEND_RELEASE_DIR"
  tar -xzf "$FRONTEND_ARTIFACT" -C "$FRONTEND_RELEASE_DIR"
  ln -sfn "$FRONTEND_RELEASE_DIR" "$FRONTEND_CURRENT_LINK"
fi

if [ -f "$NGINX_CONF_SOURCE" ]; then
  install -m 644 "$NGINX_CONF_SOURCE" "$NGINX_CONF_TARGET"
  nginx -t
  if systemctl is-active --quiet nginx 2>/dev/null; then
    systemctl reload nginx
  elif [ -s /www/server/nginx/logs/nginx.pid ]; then
    /www/server/nginx/sbin/nginx -c /www/server/nginx/conf/nginx.conf -s reload
  else
    pkill -HUP -f "/www/server/nginx/sbin/nginx -c /www/server/nginx/conf/nginx.conf"
  fi
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull backend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d backend --remove-orphans
docker image prune -f >/dev/null 2>&1 || true

echo "Redeployed OfferPilot with tag: $IMAGE_TAG"
