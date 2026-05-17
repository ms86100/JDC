#!/bin/bash
# test-dropdown-endpoints.sh
# Test all dropdown endpoints for Create Issue modal

BASE_URL="http://localhost:8080"
echo "=========================================="
echo "Testing Dropdown Endpoints"
echo "Base URL: $BASE_URL"
echo "=========================================="
echo ""

# Function to test endpoint
test_endpoint() {
    local name="$1"
    local method="$2"
    local url="$3"
    local data="$4"

    echo "--- $name ---"
    echo "Method: $method"
    echo "URL: $url"

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$url")
    else
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$data" "$url")
    fi

    http_code=$(echo "$response" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
    body=$(echo "$response" | sed '/HTTP_CODE:/d')

    echo "HTTP Status: $http_code"

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo "Status: ✅ SUCCESS"
        # Show first 500 chars of response
        echo "Response (first 500 chars):"
        echo "$body" | head -c 500
        echo ""
        echo ""
    else
        echo "Status: ❌ FAILED"
        echo "Response: $body"
        echo ""
    fi
}

echo "=========================================="
echo "1. ISSUE TYPES (jira-issue-service)"
echo "=========================================="
test_endpoint "Get Issue Types" "GET" "$BASE_URL/api/issues/types"
echo ""

echo "=========================================="
echo "2. PRIORITIES (jira-issue-service)"
echo "=========================================="
test_endpoint "Get Priorities" "GET" "$BASE_URL/api/issues/priorities"
echo ""

echo "=========================================="
echo "3. STATUSES (jira-issue-service)"
echo "=========================================="
test_endpoint "Get Statuses" "GET" "$BASE_URL/api/issues/statuses"
echo ""

echo "=========================================="
echo "4. ISSUE LINK TYPES (jira-issue-service)"
echo "=========================================="
test_endpoint "Get Link Types (standalone)" "GET" "$BASE_URL/api/issues/links/all-types"
echo ""

echo "=========================================="
echo "5. RESOLUTIONS (jira-admin-service)"
echo "=========================================="
test_endpoint "Get Resolutions" "GET" "$BASE_URL/api/admin/issues/resolutions"
echo ""

echo "=========================================="
echo "6. SECURITY LEVELS (jira-project-service)"
echo "=========================================="
test_endpoint "Get All Security Levels" "GET" "$BASE_URL/api/security-levels"
echo ""

echo "=========================================="
echo "7. VERSIONS (jira-issue-service)"
echo "=========================================="
# Get first project ID to test versions
PROJECT_ID=$(curl -s "$BASE_URL/api/projects" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
if [ -n "$PROJECT_ID" ]; then
    echo "Using Project ID: $PROJECT_ID"
    test_endpoint "Get Versions for Project" "GET" "$BASE_URL/api/versions?projectId=$PROJECT_ID"
else
    echo "No projects found - skipping versions test"
fi
echo ""

echo "=========================================="
echo "8. COMPONENTS (jira-issue-service)"
echo "=========================================="
if [ -n "$PROJECT_ID" ]; then
    echo "Using Project ID: $PROJECT_ID"
    test_endpoint "Get Components for Project" "GET" "$BASE_URL/api/components?projectId=$PROJECT_ID"
else
    echo "No projects found - skipping components test"
fi
echo ""

echo "=========================================="
echo "9. PROJECTS (jira-project-service)"
echo "=========================================="
test_endpoint "Get All Projects" "GET" "$BASE_URL/api/projects"
echo ""

echo "=========================================="
echo "10. PROJECT MEMBERS (jira-project-service)"
echo "=========================================="
if [ -n "$PROJECT_ID" ]; then
    echo "Using Project ID: $PROJECT_ID"
    test_endpoint "Get Project Members" "GET" "$BASE_URL/api/projects/$PROJECT_ID/members"
else
    echo "No projects found - skipping members test"
fi
echo ""

echo "=========================================="
echo "11. SPRINTS (jira-sprint-service - DEPRECATED)"
echo "=========================================="
test_endpoint "Get Sprints (deprecated)" "GET" "$BASE_URL/api/sprints"
echo ""

echo "=========================================="
echo "Testing Complete!"
echo "=========================================="