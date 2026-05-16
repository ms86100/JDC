#!/bin/bash

# API Endpoint Validation Script
# Tests all Jira Platform microservices directly

BASE_URL="http://localhost"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Get auth token
echo "=========================================="
echo "Getting authentication token..."
echo "=========================================="

RESPONSE=$(curl -s -X POST "$BASE_URL:8081/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
USER_ID="5ba38176-421f-431c-87f9-3836e4147a8c"

if [ -z "$TOKEN" ]; then
  echo -e "${RED}Failed to get auth token${NC}"
  exit 1
fi

echo -e "${GREEN}Token obtained successfully${NC}"
echo ""

# Function to test endpoint
test_endpoint() {
  local METHOD=$1
  local SERVICE=$2
  local PATH=$3
  local DATA=$4
  local EXPECTED=${5:-200}
  local EXTRA_HEADERS=$6

  local PORT=$(echo "$SERVICE" | grep -oP ':\K\d+')
  local URL="$BASE_URL:$PORT$PATH"

  local CMD="curl -s -w '\nHTTP: %{http_code}' -X $METHOD"

  if [ -n "$TOKEN" ]; then
    CMD="$CMD -H 'Authorization: Bearer $TOKEN'"
  fi

  if [ -n "$EXTRA_HEADERS" ]; then
    CMD="$CMD -H '$EXTRA_HEADERS'"
  fi

  if [ -n "$DATA" ]; then
    CMD="$CMD -H 'Content-Type: application/json' -d '$DATA'"
  fi

  CMD="$CMD '$URL'"

  local RESULT=$(eval "$CMD" 2>/dev/null)
  local HTTP_CODE=$(echo "$RESULT" | tail -c 4)
  local BODY=$(echo "$RESULT" | sed '$d')

  if [ "$HTTP_CODE" == "$EXPECTED" ] || [ "$HTTP_CODE" == "${EXPECTED} " ]; then
    echo -e "${GREEN}[$METHOD] $PATH -> $HTTP_CODE OK${NC}"
    return 0
  else
    echo -e "${RED}[$METHOD] $PATH -> $HTTP_CODE (expected $EXPECTED)${NC}"
    echo "  Response: ${BODY:0:100}..."
    return 1
  fi
}

echo "=========================================="
echo "Testing Auth Service (port 8081)"
echo "=========================================="
test_endpoint "POST" "8081" "/auth/login" '{"username":"admin","password":"admin123"}'
test_endpoint "POST" "8081" "/auth/register" '{"username":"testuser2","email":"test2@test.com","password":"test123456"}'

echo ""
echo "=========================================="
echo "Testing User Service (port 8082)"
echo "=========================================="
test_endpoint "GET" "8082" "/api/users/profiles"
test_endpoint "POST" "8082" "/api/users/organizations" '{"name":"Test Org 2","slug":"test-org-2"}' 201
test_endpoint "GET" "8082" "/api/users/profiles/$USER_ID"

echo ""
echo "=========================================="
echo "Testing Project Service (port 8083)"
echo "=========================================="
test_endpoint "GET" "8083" "/api/projects" "" 200 "-H 'X-User-Id: $USER_ID'"
test_endpoint "GET" "8083" "/api/projects/types"

echo ""
echo "=========================================="
echo "Testing Issue Service (port 8084)"
echo "=========================================="
test_endpoint "GET" "8084" "/api/issues"

echo ""
echo "=========================================="
echo "Testing Workflow Service (port 8085)"
echo "=========================================="
UUID_TEST="550e8400-e29b-41d4-a716-446655440000"
test_endpoint "GET" "8085" "/api/workflows/project/$UUID_TEST"

echo ""
echo "=========================================="
echo "Testing Comment Service (port 8086)"
echo "=========================================="
test_endpoint "POST" "8086" "/comments" "{\"issueId\":\"$UUID_TEST\",\"content\":\"Test comment\"}" 201 "-H 'X-User-Id: $USER_ID'"
test_endpoint "GET" "8086" "/comments/issue/$UUID_TEST"

echo ""
echo "=========================================="
echo "Testing Notification Service (port 8087)"
echo "=========================================="
test_endpoint "GET" "8087" "/api/notifications?userId=$USER_ID"

echo ""
echo "=========================================="
echo "Testing Search Service (port 8088)"
echo "=========================================="
test_endpoint "GET" "8088" "/api/search"

echo ""
echo "=========================================="
echo "Testing Audit Service (port 8089)"
echo "=========================================="
test_endpoint "GET" "8089" "/api/audit"

echo ""
echo "=========================================="
echo "Testing Attachment Service (port 8090)"
echo "=========================================="
test_endpoint "GET" "8090" "/api/attachments"

echo ""
echo "=========================================="
echo "Testing Sprint Service (port 8091)"
echo "=========================================="
test_endpoint "GET" "8091" "/api/sprints"

echo ""
echo "=========================================="
echo "Testing Plan Service (port 8092)"
echo "=========================================="
test_endpoint "GET" "8092" "/api/plans"

echo ""
echo "=========================================="
echo "Testing Admin Service (port 8093)"
echo "=========================================="
test_endpoint "GET" "8093" "/api/admin"

echo ""
echo "=========================================="
echo "Testing Migration Service (port 8094)"
echo "=========================================="
test_endpoint "GET" "8094" "/api/migration"

echo ""
echo "=========================================="
echo "Validation Complete"
echo "=========================================="