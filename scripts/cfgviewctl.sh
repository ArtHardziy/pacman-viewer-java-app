#!/usr/bin/env bash
set -euo pipefail

RUNTIME="${CFGVIEW_RUNTIME:-docker}"
IMAGE="${CFGVIEW_IMAGE:-cfgview:local}"
NAME="${CFGVIEW_NAME:-cfgview-app}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
DATA_DIR="${CFGVIEW_DATA_DIR:-${PROJECT_DIR}/data}"

mkdir -p "${DATA_DIR}"

usage() {
  cat <<'USAGE'
Usage: cfgviewctl.sh <command>

Commands:
  build         Build app jar and container image
  up            Start container in background
  stop          Stop running container
  rm            Remove container
  down          Stop and remove container
  logs          Show container logs
  status        Show container status

Optional UI env vars:
  CFGVIEW_UI_SCALE          (example: 1.5)
  CFGVIEW_FONT_SCALE        (example: 1.35)
  CFGVIEW_TABLE_ROW_HEIGHT  (example: 34)
  CFGVIEW_WINDOW_WIDTH      (example: 2200)
  CFGVIEW_WINDOW_HEIGHT     (example: 1300)
USAGE
}

ensure_display() {
  if [[ -z "${DISPLAY:-}" ]]; then
    echo "DISPLAY is empty. Start from GUI session (X11/Wayland with XWayland)." >&2
    exit 1
  fi

  if command -v xhost >/dev/null 2>&1; then
    xhost +SI:localuser:"$(id -un)" >/dev/null 2>&1 || true
  fi
}

build_all() {
  (cd "${PROJECT_DIR}" && mvn -DskipTests package)
  if [[ "${RUNTIME}" == "podman" ]]; then
    (cd "${PROJECT_DIR}" && podman build -f Containerfile -t "${IMAGE}" .)
  else
    (cd "${PROJECT_DIR}" && docker build -t "${IMAGE}" .)
  fi
}

up() {
  ensure_display

  if [[ "${RUNTIME}" == "podman" ]]; then
    podman rm -f "${NAME}" >/dev/null 2>&1 || true
    podman run -d \
      --name "${NAME}" \
      --userns keep-id \
      -e DISPLAY="${DISPLAY}" \
      -e CFGVIEW_UI_SCALE \
      -e CFGVIEW_FONT_SCALE \
      -e CFGVIEW_TABLE_ROW_HEIGHT \
      -e CFGVIEW_WINDOW_WIDTH \
      -e CFGVIEW_WINDOW_HEIGHT \
      -v /tmp/.X11-unix:/tmp/.X11-unix:rw \
      -v "${DATA_DIR}:/app/data" \
      -v "${PROJECT_DIR}:/workspace:ro" \
      -w /app/data \
      "${IMAGE}"
  else
    docker rm -f "${NAME}" >/dev/null 2>&1 || true
    docker run -d \
      --name "${NAME}" \
      --user "$(id -u):$(id -g)" \
      -e DISPLAY="${DISPLAY}" \
      -e CFGVIEW_UI_SCALE \
      -e CFGVIEW_FONT_SCALE \
      -e CFGVIEW_TABLE_ROW_HEIGHT \
      -e CFGVIEW_WINDOW_WIDTH \
      -e CFGVIEW_WINDOW_HEIGHT \
      -v /tmp/.X11-unix:/tmp/.X11-unix:rw \
      -v "${DATA_DIR}:/app/data" \
      -v "${PROJECT_DIR}:/workspace:ro" \
      -w /app/data \
      "${IMAGE}"
  fi
}

stop() {
  if [[ "${RUNTIME}" == "podman" ]]; then
    podman stop "${NAME}" || true
  else
    docker stop "${NAME}" || true
  fi
}

rm_container() {
  if [[ "${RUNTIME}" == "podman" ]]; then
    podman rm -f "${NAME}" || true
  else
    docker rm -f "${NAME}" || true
  fi
}

logs() {
  if [[ "${RUNTIME}" == "podman" ]]; then
    podman logs -f "${NAME}"
  else
    docker logs -f "${NAME}"
  fi
}

status() {
  if [[ "${RUNTIME}" == "podman" ]]; then
    podman ps -a --filter "name=${NAME}"
  else
    docker ps -a --filter "name=${NAME}"
  fi
}

cmd="${1:-}"
case "${cmd}" in
  build) build_all ;;
  up) up ;;
  stop) stop ;;
  rm) rm_container ;;
  down) stop; rm_container ;;
  logs) logs ;;
  status) status ;;
  *) usage; exit 1 ;;
esac
