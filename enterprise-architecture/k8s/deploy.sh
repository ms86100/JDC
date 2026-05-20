#!/bin/bash
# Jira Platform Kubernetes Deployment Script
# Usage: ./deploy.sh [environment] [action]
# Examples:
#   ./deploy.sh prod apply
#   ./deploy.sh staging delete

set -e

ENVIRONMENT=${1:-prod}
ACTION=${2:-apply}

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    echo_info "Checking prerequisites..."

    command -v kubectl >/dev/null 2>&1 || { echo_error "kubectl is required but not installed."; exit 1; }
    command -v helm >/dev/null 2>&1 || { echo_warn "helm is not installed. Helm charts will not be deployed."; }

    kubectl cluster-info >/dev/null 2>&1 || { echo_error "kubectl is not configured."; exit 1; }
}

# Create namespace and base resources
deploy_base() {
    echo_info "Deploying base resources..."
    kubectl $ACTION -f namespace.yaml

    echo_info "Deploying ConfigMaps..."
    kubectl $ACTION -f configmaps/

    echo_info "Deploying Secrets..."
    kubectl $ACTION -f secrets/

    echo_info "Deploying ServiceAccount..."
    kubectl $ACTION -f service-account.yaml
}

# Deploy services
deploy_services() {
    echo_info "Deploying Services..."
    kubectl $ACTION -f services/

    echo_info "Deploying Deployments..."
    kubectl $ACTION -f deployments/

    echo_info "Deploying Ingress & Network policies..."
    kubectl $ACTION -f networking/

    echo_info "Deploying Monitoring..."
    kubectl $ACTION -f monitoring/

    echo_info "Deploying Storage..."
    kubectl $ACTION -f storage/
}

# Verify deployment
verify_deployment() {
    echo_info "Verifying deployment..."

    echo "Checking Pods..."
    kubectl get pods -n jira-platform

    echo ""
    echo "Checking Services..."
    kubectl get svc -n jira-platform

    echo ""
    echo "Checking Deployments..."
    kubectl get deployments -n jira-platform

    echo ""
    echo "Checking HPA..."
    kubectl get hpa -n jira-platform

    echo ""
    echo "Checking Ingress..."
    kubectl get ingress -n jira-platform
}

# Main deployment
main() {
    echo_info "Starting Jira Platform deployment..."
    echo_info "Environment: $ENVIRONMENT"
    echo_info "Action: $ACTION"

    check_prerequisites

    # Change to k8s directory
    cd "$(dirname "$0")"

    deploy_base
    deploy_services
    verify_deployment

    echo_info "Deployment complete!"
    echo ""
    echo "Access the platform at:"
    echo "  - API: https://api.platform.local"
    echo "  - UI: https://platform.local"
}

main "$@"