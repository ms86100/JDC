# Kubernetes & Helm Deployment Guide

## Phase 18 - DevOps & Deployment

This directory contains Kubernetes manifests and Helm charts for deploying the Jira Platform to production.

## Directory Structure

```
enterprise-architecture/
├── k8s/                           # Kubernetes manifests
│   ├── issue-service/
│   │   ├── deployment.yaml        # Issue service deployment
│   │   └── ...
│   ├── gateway/
│   └── ...
├── helm/
│   └── jira-platform/             # Helm chart
│       ├── Chart.yaml
│       ├── values.yaml
│       └── charts/
└── README.md
```

## Prerequisites

1. Kubernetes 1.28+
2. Helm 3.12+
3. kubectl configured
4. Container registry access
5. Ingress controller (nginx)
6. Cert-manager for TLS

## Quick Start

### Using Helm

```bash
# Add the repository
helm repo add jira-platform https://charts.example.com

# Update repositories
helm repo update

# Install the chart
helm install jira-platform jira-platform/jira-platform \
  --namespace jira-platform \
  --create-namespace \
  -f values.yaml
```

### Using kubectl directly

```bash
# Apply namespace
kubectl apply -f namespace.yaml

# Apply configurations
kubectl apply -f configmaps/
kubectl apply -f secrets/

# Apply deployments
kubectl apply -f k8s/
```

## Configuration

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| DB_HOST | PostgreSQL host | jira-postgres |
| DB_PORT | PostgreSQL port | 5432 |
| DB_NAME | Database name | jira_platform |
| DB_USERNAME | Database user | jiraadmin |
| REDIS_HOST | Redis host | jira-redis |
| KAFKA_BOOTSTRAP_SERVERS | Kafka brokers | jira-kafka:9092 |
| EUREKA_SERVER_URL | Service discovery | http://jira-eureka:8761 |

### Resource Sizing

| Service | CPU Request | Memory Request | CPU Limit | Memory Limit |
|---------|-------------|----------------|-----------|--------------|
| Issue Service | 250m | 512Mi | 1000m | 1Gi |
| Gateway | 500m | 512Mi | 2000m | 2Gi |
| PostgreSQL | 500m | 1Gi | 1000m | 2Gi |
| Redis | 250m | 512Mi | 500m | 1Gi |

## Scaling

### Horizontal Pod Autoscaling

```bash
# View HPA
kubectl get hpa -n jira-platform

# Manual scale
kubectl scale deployment jira-issue-service -n jira-platform --replicas=5
```

### Cluster Autoscaling

For GKE/EKS/AKS, configure cluster autoscaler to work with the HPA settings.

## Monitoring

### Prometheus Metrics

All services expose metrics at `/actuator/prometheus`:

- `jira_tests_total` - Total tests count
- `jira_test_executions_total` - Total executions
- `jira_test_pass_rate` - Current pass rate
- `jira_execution_duration_seconds` - Execution duration histogram

### Grafana Dashboards

Import dashboards from `monitoring/dashboards/`.

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -n jira-platform -l app=jira-issue-service
kubectl describe pod <pod-name> -n jira-platform
kubectl logs <pod-name> -n jira-platform
```

### Check Logs

```bash
kubectl logs -n jira-platform -l app=jira-issue-service --tail=100 -f
```

### Port Forward for Local Testing

```bash
kubectl port-forward -n jira-platform svc/jira-issue-service 8084:8084
```

## Security

### Network Policies

Enable network policies in values.yaml:

```yaml
networkPolicies:
  enabled: true
```

### Pod Security

All pods run with non-root user (UID 1000).

### Secrets Management

Use external secrets operator or cloud provider secrets manager:

```yaml
# Example: External Secret
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: jira-db-secret
spec:
  secretStoreRef:
    name: vault-backend
    kind: VaultStore
  target:
    name: jira-db-secret
  data:
    - secretKey: DB_PASSWORD
      remoteRef:
        key: prod/jira/database
        property: password
```

## High Availability

### Multi-AZ Deployment

Configure podAntiAffinity for high availability:

```yaml
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
            - key: app
              operator: In
              values:
                - jira-issue-service
        topologyKey: topology.kubernetes.io/zone
```

### Disaster Recovery

Backups are configured via PostgreSQL and Redis persistence.
Enable cross-region replication for production.

## CI/CD Integration

### ArgoCD Example

```yaml
# Application definition
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: jira-platform
spec:
  project: default
  source:
    repoURL: https://github.com/example/jira-platform
    targetRevision: HEAD
    path: enterprise-architecture/helm/jira-platform
  destination:
    server: https://kubernetes.default.svc
    namespace: jira-platform
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

### GitHub Actions Example

```yaml
name: Deploy to Kubernetes
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build and Push Docker Image
        run: |
          docker build -t jira-platform/jira-issue-service:${{ github.sha }} .
          docker push jira-platform/jira-issue-service:${{ github.sha }}
      - name: Deploy to Kubernetes
        run: |
          helm upgrade --install jira-platform ./enterprise-architecture/helm/jira-platform \
            --set services.issue-service.image.tag=${{ github.sha }}
```

## License

Copyright 2024 Jira Platform Team