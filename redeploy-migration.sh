#!/bin/bash
# Run this after restarting Docker Desktop to deploy the fixed migration service.
set -e
cd "$(dirname "$0")/avionics-systems-migration-service"

echo "=== Building fixed migration-service image (includes Maven build + Netskope cert) ==="
docker build -f Dockerfile.build-and-package -t avionics-systems-migration-service:latest .

echo "=== Recreating migration-service container with new image ==="
cd ..
docker-compose -f docker-compose.core.yml up -d --no-deps migration-service

echo "=== Waiting 60s for Spring Boot to start... ==="
sleep 60

echo "=== Verifying GET /fields/custom ==="
curl -s "http://localhost:3000/fields/custom" | node -e "let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{try{const a=JSON.parse(d); console.log('GET OK:', a.length,'fields');}catch(e){console.log('Error:', d.substr(0,100));}})"

echo ""
echo "=== Testing POST /fields/custom ==="
curl -s -m 30 -X POST "http://localhost:3000/fields/custom" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test After Redeploy","type":"com.atlassian.jira.plugin.system.customfieldtypes:textfield"}' | \
  node -e "let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{try{const r=JSON.parse(d); console.log('POST OK: created field:', r.name);}catch(e){console.log('Error:', d.substr(0,100));}})"
