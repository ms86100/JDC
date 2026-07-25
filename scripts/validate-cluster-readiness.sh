#!/bin/bash
# Validate that docker-compose.yml is cluster-safe.
# Run: bash scripts/validate-cluster-readiness.sh [docker-compose.yml]

set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.yml}"
ERRORS=0
WARNINGS=0

INFRA_PATTERN="postgres|redis|minio|zipkin|frontend"

echo "=== Cluster Readiness Validator ==="
echo "Checking: $COMPOSE_FILE"
echo ""

# Check for container_name on non-infrastructure services
echo "--- Rule R5: No container_name on application services ---"
while IFS= read -r service; do
  service=$(echo "$service" | xargs | sed 's/:$//')
  if [[ -z "$service" ]] || echo "$service" | grep -qiE "^($INFRA_PATTERN)"; then
    continue
  fi
  if grep -A 20 "^  ${service}:" "$COMPOSE_FILE" | grep -q "container_name:"; then
    echo "  ERROR: Service '$service' has container_name (prevents scaling)"
    ERRORS=$((ERRORS + 1))
  fi
done < <(grep -E '^\s{2}[a-z].*:$' "$COMPOSE_FILE" | grep -v '#' | sed 's/^\s*//')

# Check for fixed port mappings on non-infrastructure services
echo ""
echo "--- Rule R6: No fixed host port mappings on application services ---"
while IFS= read -r service; do
  service=$(echo "$service" | xargs | sed 's/:$//')
  if [[ -z "$service" ]] || echo "$service" | grep -qiE "^($INFRA_PATTERN|gateway)"; then
    continue
  fi
  if grep -A 30 "^  ${service}:" "$COMPOSE_FILE" | grep -E '^\s+-\s*"[0-9]+:[0-9]+"' | head -1 > /dev/null 2>&1; then
    ports=$(grep -A 30 "^  ${service}:" "$COMPOSE_FILE" | grep -E '^\s+-\s*"[0-9]+:[0-9]+"' | head -1 | xargs)
    echo "  ERROR: Service '$service' has fixed port mapping: $ports"
    ERRORS=$((ERRORS + 1))
  fi
done < <(grep -E '^\s{2}[a-z].*:$' "$COMPOSE_FILE" | grep -v '#' | sed 's/^\s*//')

# Check for required infrastructure
echo ""
echo "--- Infrastructure Check ---"
if ! grep -q "redis:" "$COMPOSE_FILE"; then
  echo "  WARNING: Redis not found in compose file (needed for distributed cache/locking)"
  WARNINGS=$((WARNINGS + 1))
fi
if ! grep -q "minio:" "$COMPOSE_FILE"; then
  echo "  WARNING: MinIO not found in compose file (needed for shared storage)"
  WARNINGS=$((WARNINGS + 1))
fi

echo ""
echo "=== Results ==="
echo "Errors:   $ERRORS"
echo "Warnings: $WARNINGS"

if [ "$ERRORS" -gt 0 ]; then
  echo "FAILED: Fix errors before deploying in cluster mode."
  exit 1
fi

echo "PASSED (with $WARNINGS warnings)"
exit 0
