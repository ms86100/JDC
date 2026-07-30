#!/bin/bash
# =============================================================================
# Pre-build Dependency JARs Script
# Builds cross-service dependencies that are required by other services
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CACHE_DIR="$PROJECT_DIR/.m2-cache"

# Services that need to be pre-built (they are dependencies of other services)
DEPENDENCY_SERVICES=(
    "avionics-systems-admin-service"
    "avionics-systems-project-service"
)

echo "==============================================="
echo "  Pre-building Dependency JARs for Docker"
echo "==============================================="
echo ""

# Create Maven cache directory
mkdir -p "$CACHE_DIR"
echo "Using local Maven cache at: $CACHE_DIR"
echo ""

# Build each dependency service and copy JAR to cache
for SERVICE in "${DEPENDENCY_SERVICES[@]}"; do
    SERVICE_DIR="$PROJECT_DIR/$SERVICE"

    if [ ! -d "$SERVICE_DIR" ]; then
        echo "[ERROR] Service directory not found: $SERVICE_DIR"
        exit 1
    fi

    echo "----------------------------------------"
    echo "Building: $SERVICE"
    echo "----------------------------------------"

    # Build the service with Maven
    # -DskipTests: Skip tests for faster builds
    # -Dmaven.repo.local: Use local cache directory
    cd "$SERVICE_DIR"
    mvn clean package -DskipTests -Dmaven.repo.local="$CACHE_DIR" -q

    # Find the built JAR
    JAR_FILE=$(find "$SERVICE_DIR/target" -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" 2>/dev/null | head -1)

    if [ -z "$JAR_FILE" ]; then
        echo "[ERROR] Failed to build JAR for $SERVICE"
        exit 1
    fi

    echo "Built: $JAR_FILE"
    echo ""
done

echo "==============================================="
echo "  Pre-build Complete"
echo "==============================================="
echo ""
echo "JARs available at: $CACHE_DIR"
echo ""

# List cached JARs
echo "Cached JARs:"
find "$CACHE_DIR" -name "*.jar" 2>/dev/null | while read jar; do
    echo "  - $(basename "$jar")"
done

echo ""
echo "You can now build services that depend on these JARs."
echo "The Dockerfile will automatically copy JARs from .m2-cache"