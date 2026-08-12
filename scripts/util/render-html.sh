#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE_NAME=""
DATA_FILE=""
OUTPUT_PATH=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --template)
      TEMPLATE_NAME="${2:-}"
      shift 2
      ;;
    --data)
      DATA_FILE="${2:-}"
      shift 2
      ;;
    --out)
      OUTPUT_PATH="${2:-}"
      shift 2
      ;;
    --with-redaction)
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -z "$TEMPLATE_NAME" || -z "$DATA_FILE" || -z "$OUTPUT_PATH" ]]; then
  echo "Error: Missing required arguments" >&2
  exit 1
fi

case "$TEMPLATE_NAME" in
  accept|plan-brief|progress)
    TEMPLATE_FILE="$SCRIPT_DIR/../templates/html/${TEMPLATE_NAME}.html.template"
    ;;
  *)
    echo "Error: Unsupported template: $TEMPLATE_NAME" >&2
    exit 1
    ;;
esac

if [[ ! -f "$TEMPLATE_FILE" ]]; then
  echo "Error: Template file not found: $TEMPLATE_FILE" >&2
  exit 1
fi

if [[ ! -f "$DATA_FILE" ]]; then
  echo "Error: Data file not found: $DATA_FILE" >&2
  exit 1
fi

mkdir -p "$(dirname -- "$OUTPUT_PATH")"
node "$SCRIPT_DIR/render-template.js" \
  --template "$TEMPLATE_FILE" \
  --data "$DATA_FILE" \
  --out "$OUTPUT_PATH"

echo "HTML generated: $OUTPUT_PATH"
