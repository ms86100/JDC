#!/bin/bash
# Start all JDC platform services with remote DB
# Usage: bash start-all-services.sh [stop|status]

BASE="c:/Users/SSHABNSA/Desktop/test/JDC-main"
LOGDIR="/tmp/jdc-logs"
mkdir -p "$LOGDIR"

DB_URL="jdbc:postgresql://in0-eplmdb-v01:5432/systems"
DB_USER="systems_admin"
DB_PASS="Hcu4ieD8R13qaf7JVSsu"

COMMON_ARGS="-Dspring.datasource.url=$DB_URL -Dspring.datasource.username=$DB_USER -Dspring.datasource.password=$DB_PASS -Dspring.flyway.url=$DB_URL -Dspring.flyway.user=$DB_USER -Dspring.flyway.password=$DB_PASS -Dspring.flyway.validate-on-migrate=false -Djira.permissions.fail-open=true -Dspring.jpa.hibernate.ddl-auto=none -Dmanagement.health.redis.enabled=false -Dmigration.security.enabled=false -Dspring.datasource.hikari.maximum-pool-size=5 -Dspring.datasource.hikari.minimum-idle=1"

stop_all() {
  echo "Stopping all services..."
  taskkill //F //IM java.exe 2>/dev/null
  taskkill //F //IM node.exe 2>/dev/null
  sleep 3
  echo "All stopped."
}

check_status() {
  echo "=== Service Status ==="
  for s in "8080:gateway" "8081:auth" "8082:user" "8083:project" "8084:issue" "8085:workflow" "8086:comment" "8087:notification" "8088:search" "8089:audit" "8090:attachment" "8091:sprint" "8092:plan" "8093:admin" "8094:migration" "8095:test" "8096:dashboard" "8097:component" "8098:report" "8099:version" "3000:frontend"; do
    port=$(echo $s | cut -d: -f1); name=$(echo $s | cut -d: -f2)
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://localhost:${port}/actuator/health" 2>/dev/null)
    if [ "$code" = "200" ]; then echo "  $name ($port): UP"
    elif [ "$code" = "000" ]; then echo "  $name ($port): DOWN"
    else echo "  $name ($port): HTTP $code"; fi
  done
}

start_service() {
  local name=$1 jar=$2 port=$3 heap=${4:-256m} extra=${5:-}
  echo -n "  Starting $name ($port)... "
  nohup java -Xms$heap -Xmx$heap $COMMON_ARGS $extra -jar "$jar" --server.port=$port > "$LOGDIR/${name}.log" 2>&1 &
  echo "PID $!"
}

start_plan_service() {
  echo -n "  Starting plan-service (8092) from compiled classes... "
  cd "$BASE/jira-plan-service"
  CP="target/classes"
  for jar in target/deps/BOOT-INF/lib/*.jar; do CP="$CP;$jar"; done
  nohup java -Xms256m -Xmx256m $COMMON_ARGS -cp "$CP" com.jira.plan.JiraPlanServiceApplication --server.port=8092 > "$LOGDIR/plan.log" 2>&1 &
  echo "PID $!"
  cd "$BASE"
}

if [ "$1" = "stop" ]; then stop_all; exit 0; fi
if [ "$1" = "status" ]; then check_status; exit 0; fi

echo ""
echo "============================================="
echo "  JDC PLATFORM - Starting All Services"
echo "============================================="
echo ""

echo "--- WAVE 1: Backend services ---"
start_service "auth"         "$BASE/jira-auth-service/target/jira-auth-service-1.0.0.jar"               8081 256m
start_service "user"         "$BASE/jira-user-service/target/jira-user-service-1.0.0.jar"               8082 256m
start_service "project"      "$BASE/jira-project-service/target/jira-project-service-1.0.0.jar"         8083 256m
start_service "issue"        "$BASE/jira-issue-service/target/jira-issue-service-1.0.0.jar"             8084 512m
start_service "workflow"     "$BASE/jira-workflow-service/target/jira-workflow-service-1.0.0.jar"        8085 256m
start_service "comment"      "$BASE/jira-comment-service/target/jira-comment-service-1.0.0.jar"         8086 256m
start_service "notification" "$BASE/jira-notification-service/target/jira-notification-service-1.0.0.jar" 8087 256m
start_service "search"       "$BASE/jira-search-service/target/jira-search-service-1.0.0.jar"           8088 256m
start_service "audit"        "$BASE/jira-audit-service/target/jira-audit-service-1.0.0.jar"             8089 256m
start_service "attachment"   "$BASE/jira-attachment-service/target/jira-attachment-service-1.0.0.jar"    8090 512m
start_service "sprint"       "$BASE/jira-sprint-service/target/jira-sprint-service-1.0.0.jar"           8091 256m
start_plan_service
start_service "admin"        "$BASE/jira-admin-service/target/jira-admin-service-1.0.0.jar"             8093 256m
start_service "migration"    "$BASE/jira-migration-service/target/jira-migration-service-1.0.0.jar"     8094 512m
start_service "test"         "$BASE/jira-test-service/target/jira-test-service-1.0.0.jar"               8095 256m
start_service "dashboard"    "$BASE/jira-dashboard-service/target/jira-dashboard-service-1.0.0.jar"     8096 256m
start_service "component"    "$BASE/jira-component-service/target/jira-component-service-1.0.0.jar"     8097 256m
start_service "report"       "$BASE/jira-report-service/target/jira-report-service-1.0.0.jar"           8098 256m
start_service "version"      "$BASE/jira-version-service/target/jira-version-service-1.0.0.jar"         8099 256m

echo ""
echo "--- WAVE 2: Gateway ---"
sleep 5
start_service "gateway"      "$BASE/jira-gateway/target/jira-gateway-1.0.0.jar"                        8080 512m

echo ""
echo "--- WAVE 3: Frontend ---"
sleep 3
echo -n "  Starting frontend (3000)... "
cd "$BASE/jira-frontend" && nohup npm run dev > "$LOGDIR/frontend.log" 2>&1 &
echo "PID $!"
cd "$BASE"

echo ""
echo "============================================="
echo "  All 21 services launched!"
echo "  Waiting 60s for startup..."
echo "============================================="
sleep 60
echo ""
check_status
echo ""
echo "  Frontend: http://localhost:3000"
echo "  Gateway:  http://localhost:8080"
echo "  Logs:     $LOGDIR/"
