#!/bin/bash
# Full Platform Rebuild Script
# Handles OneDrive file locking issues

set -e
cd /c/Users/thech/OneDrive/Desktop/cloudetest/avionics-systems-platform

echo "=== Stopping all Java services ==="
for pid in $(netstat -ano 2>/dev/null | grep -E ":808[0-9]|:809[0-9]" | grep LISTENING | awk '{print $5}' | sort -u); do
  taskkill //F //PID $pid 2>/dev/null || true
done
sleep 5

echo "=== Rebuilding services ==="
services=("avionics-systems-plan-service" "avionics-systems-admin-service" "avionics-systems-notification-service" "avionics-systems-gateway")

for service in "${services[@]}"; do
  echo "Building $service..."
  cd "$service"
  rm -f target/*.original 2>/dev/null || true
  rm -f target/*.jar 2>/dev/null || true
  mvn clean package -DskipTests -q 2>&1 | tail -3
  cd ..
done

echo "=== Starting all services ==="
for service_dir in avionics-systems-auth-service avionics-systems-user-service avionics-systems-project-service avionics-systems-issue-service \
                   avionics-systems-workflow-service avionics-systems-comment-service avionics-systems-notification-service \
                   avionics-systems-search-service avionics-systems-audit-service avionics-systems-attachment-service \
                   avionics-systems-sprint-service avionics-systems-plan-service avionics-systems-admin-service \
                   avionics-systems-migration-service avionics-systems-gateway; do
  service_name=$(basename "$service_dir")
  jar_file="$service_dir/target/${service_name}-1.0.0.jar"
  if [ -f "$jar_file" ]; then
    nohup java -jar "$jar_file" --spring.profiles.active=local > "/tmp/${service_name}.log" 2>&1 &
    echo "Started $service_name"
  fi
done

echo "=== Waiting for services to initialize ==="
sleep 45

echo "=== Service Status ==="
for port in 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
  name=""
  case $port in 8080) name="gateway";; 8081) name="auth";; 8082) name="user";; 8083) name="project";;
    8084) name="issue";; 8085) name="workflow";; 8086) name="comment";; 8087) name="notification";;
    8088) name="search";; 8089) name="audit";; 8090) name="attachment";; 8091) name="sprint";;
    8092) name="plan";; 8093) name="admin";; 8094) name="migration";; esac
  echo "Port $port ($name): $status"
done

echo "=== Testing fixed endpoints ==="
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' 2>/dev/null | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

echo "Testing POST /api/plans/programs..."
curl -s -w "\nHTTP: %{http_code}" -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -H "X-User-Id: 5ba38176-421f-431c-87f9-3836e4147a8c" \
  http://localhost:8080/api/plans/programs -d '{"name":"Test Program","description":"Test"}' 2>/dev/null

echo ""
echo "=== Done ==="