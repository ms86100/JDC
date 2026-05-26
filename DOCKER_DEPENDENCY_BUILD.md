# Docker Build with Pre-built Dependencies

## Problem

When building individual services via Docker, cross-service dependencies (like `jira-issue-service` depending on `jira-project-service` and `jira-admin-service`) cause build failures because the dependent JARs are not available during Docker build.

## Solution: Pre-build Dependency JARs

The solution involves:
1. Building dependency JARs locally before Docker build
2. Making them available to the Docker build context via a cache directory
3. Installing them into Maven local repository during the Docker build

## Files Created/Modified

### 1. `scripts/prebuild-dependencies.sh` (NEW)
A standalone script that:
- Builds `jira-admin-service` and `jira-project-service` JARs locally
- Copies them to `.m2-cache/` directory
- Can be run independently before Docker builds

### 2. `scripts/docker-build-with-deps.sh` (NEW)
A convenience script that:
- Runs the prebuild step automatically
- Then executes `docker compose build`
- Provides a single command for complete builds

### 3. `jira-issue-service/Dockerfile` (MODIFIED)
Updated to:
- Copy pre-built JARs from `.dependency-jars/` to `/tmp/deps/`
- Install them into Maven local repository using `mvn install:install-file`
- This makes them available during Maven dependency resolution

### 4. `.dockerignore` (MODIFIED)
Extended to exclude:
- Logs and temporary files
- IDE files (.idea, .vscode)
- Git files
- Build artifacts (target directories)
- node_modules
- *.log files

## Usage

### Option 1: Run prebuild script then docker compose
```bash
# First, build dependencies
./scripts/prebuild-dependencies.sh

# Then build all Docker images
docker compose build

# Or build specific service
docker compose build issue-service
```

### Option 2: Use the combined script
```bash
./scripts/docker-build-with-deps.sh
```

### Option 3: CI/CD Pipeline
```bash
# In your CI/CD pipeline
- name: Build dependency JARs
  run: ./scripts/prebuild-dependencies.sh

- name: Build Docker images
  run: docker compose build
```

## How It Works

1. **Prebuild Phase**: Maven builds `jira-admin-service` and `jira-project-service` locally, producing JARs in their `target/` directories.

2. **Cache Phase**: The JARs are copied to `.dependency-jars/` with simplified names:
   - `admin-service.jar`
   - `project-service.jar`

3. **Docker Build Phase**: The Dockerfile:
   - Copies `.dependency-jars/` into the build context
   - Uses `mvn install:install-file` to install each JAR into the Maven local repository
   - Then runs the normal Maven build which can now resolve cross-service dependencies

4. **Runtime Phase**: The JARs are bundled into the final Docker image and the service starts normally.

## Notes

- The pre-built JARs are installed with version `1.0.0` matching the parent pom.xml
- The `.dockerignore` includes `.dependency-jars/` to prevent unnecessary rebuilds
- The approach is non-destructive - existing builds still work
- Can be extended to support more dependency services as needed

## Alternative Approaches Considered

1. **Maven Repository Server**: Would require infrastructure setup
2. **Multi-stage with Build Dependencies**: Already in place but slow
3. **Pre-built JAR volumes**: More complex to manage

The current approach was chosen for simplicity and minimal infrastructure requirements.