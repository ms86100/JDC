# Jira Test Management Plugin

Atlassian Marketplace-ready plugin for Xray Test Management clone functionality.

## Features

- **Test Issue Type**: Native Test, Test Set, and Test Plan issue types
- **Custom Fields**: Test Type, Test Steps, Test Status
- **REST API**: Full test management API at `/rest/test-management/1.0/`
- **Reports**: Test Summary, Coverage, and Execution History reports
- **Workflow Integration**: Custom conditions and post-functions for test workflows
- **Event Listeners**: Track test creation, updates, and deletions
- **Import Support**: Cucumber/Gherkin and JUnit XML import

## Build

```bash
cd jira-marketplace-plugin
mvn clean package
```

## Install

Upload the generated JAR to your Jira instance:

1. Navigate to **Jira Settings > Apps**
2. Click **Upload app**
3. Select `target/jira-test-management-plugin-1.0.0.jar`

## Configuration

After installation:

1. Go to **Jira Settings > Apps > Test Management**
2. Configure default test project
3. Set up CI/CD webhook integrations

## API Endpoints

```
POST   /rest/test-management/1.0/tests
GET    /rest/test-management/1.0/tests/{id}
PUT    /rest/test-management/1.0/tests/{id}
DELETE /rest/test-management/1.0/tests/{id}

POST   /rest/test-management/1.0/test-sets
POST   /rest/test-management/1.0/test-sets/{id}/tests

POST   /rest/test-management/1.0/test-plans
POST   /rest/test-management/1.0/test-plans/{id}/execute

POST   /rest/test-management/1.0/test-executions
GET    /rest/test-management/1.0/test-executions/{id}
PUT    /rest/test-management/1.0/test-executions/{id}/steps/{stepId}

POST   /rest/test-management/1.0/import/cucumber
POST   /rest/test-management/1.0/import/junit

GET    /rest/test-management/1.0/reports/summary
GET    /rest/test-management/1.0/reports/coverage
```

## Requirements

- Jira 9.3.0 or later
- Java 17
- Maven 3.9+

## License

Proprietary - Jira Platform Team