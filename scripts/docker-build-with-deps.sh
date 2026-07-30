#!/bin/bash
# =============================================================================
# Docker Build with Pre-built Dependencies
#
# This script first builds dependency JARs (avionics-systems-admin-service, avionics-systems-project-service)
# and then runs Docker build, making JARs available via a build cache volume.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Dependency services that need to be pre-built
DEPENDENCY_SERVICES=(
    "avionics-systems-admin-service"
    "avionics-systems-project-service"
)

echo "==============================================="
echo "  Docker Build with Dependency Pre-building"
echo "==============================================="
echo ""

# Step 1: Build dependency JARs
echo "[Step 1] Building dependency JARs..."
echo ""

# Create cache directory
CACHE_DIR="$PROJECT_DIR/.dependency-jars"
mkdir -p "$CACHE_DIR"

# Build each dependency service
for SERVICE in "${DEPENDENCY_SERVICES[@]}"; do
    SERVICE_DIR="$PROJECT_DIR/$SERVICE"

    if [ ! -d "$SERVICE_DIR" ]; then
        echo "[ERROR] Service directory not found: $SERVICE_DIR"
        exit 1
    fi

    echo "Building $SERVICE..."

    # Build the service with Maven
    cd "$SERVICE_DIR"
    mvn clean package -DskipTests -q

    # Copy JAR to cache directory (without version in name for easier reference)
    JAR_FILE=$(find "$SERVICE_DIR/target" -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" 2>/dev/null | head -1)

    if [ -n "$JAR_FILE" ]; then
        CACHED_JAR="$CACHE_DIR/${SERVICE##avionics-systems-}-service.jar"
        cp "$JAR_FILE" "$CACHED_JAR"
        echo "  -> Cached: $CACHED_JAR"
    fi
done

echo ""
echo "[Step 2] Running Docker build..."
echo ""

# Step 2: Run Docker build (the Dockerfile will copy from .dependency-jars)
cd "$PROJECT_DIR"
docker compose build --no-cache

echo ""
echo "==============================================="
echo "  Build Complete"
echo "==============================================="