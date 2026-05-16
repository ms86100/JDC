# Prisma Schema Enterprise Update Guide

## Overview

This guide maps the enterprise database features to Prisma schema for type-safe access.

## Schema Files Location

- Main Prisma schema: `jira-*/prisma/schema.prisma`
- Each service has its own Prisma schema

## V21 Enterprise Features → Prisma Mapping

### 1. Permission Schemes (jira-admin-service)

```prisma
// PermissionScheme
model PermissionScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  projects    ProjectPermissionScheme[]
  grants      PermissionSchemeGrant[]

  @@map("permission_schemes")
}

// Project to Permission Scheme mapping
model ProjectPermissionScheme {
  id                  String   @id @default(uuid())
  projectId           String
  permissionSchemeId  String
  createdAt           DateTime @default(now())

  project            Project  @relation(fields: [projectId], references: [id])
  permissionScheme   PermissionScheme @relation(fields: [permissionSchemeId], references: [id])

  @@unique([projectId])
  @@map("project_permission_schemes")
}

// Standard Jira Permissions
model Permission {
  id              String   @id @default(uuid())
  permissionKey   String   @unique
  permissionType  String   // 'PROJECT', 'ISSUE', 'GLOBAL'
  description     String?
  createdAt       DateTime @default(now())

  grants          PermissionSchemeGrant[]
  rolePermissions RolePermission[]

  @@map("permissions")
}

// Permission Grant (who gets what permission)
model PermissionSchemeGrant {
  id                   String   @id @default(uuid())
  permissionSchemeId   String
  permissionId         String
  holderType           String   // 'USER', 'GROUP', 'PROJECT_ROLE'
  holderId             String?
  createdAt            DateTime @default(now())

  permissionScheme     PermissionScheme @relation(fields: [permissionSchemeId], references: [id])
  permission           Permission @relation(fields: [permissionId], references: [id])

  @@unique([permissionSchemeId, permissionId, holderType, holderId])
  @@map("permission_scheme_grants")
}

// Project Roles
model ProjectRole {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  permissions RolePermission[]
  actors       ProjectRoleActor[]

  @@map("project_roles")
}

// Role Permissions
model RolePermission {
  id            String   @id @default(uuid())
  projectRoleId String
  permissionId  String
  createdAt     DateTime @default(now())

  projectRole   ProjectRole @relation(fields: [projectRoleId], references: [id])
  permission    Permission  @relation(fields: [permissionId], references: [id])

  @@unique([projectRoleId, permissionId])
  @@map("role_permissions")
}

// User/Group to Role assignment
model ProjectRoleActor {
  id            String   @id @default(uuid())
  projectId     String
  projectRoleId String
  holderType    String   // 'USER', 'GROUP'
  holderId      String
  createdAt     DateTime @default(now())

  project       Project    @relation(fields: [projectId], references: [id])
  projectRole   ProjectRole @relation(fields: [projectRoleId], references: [id])

  @@unique([projectId, projectRoleId, holderType, holderId])
  @@map("project_role_actors")
}

// Groups
model Group {
  id          String   @id @default(uuid())
  groupName   String   @unique
  description String?
  createdAt   DateTime @default(now())

  memberships UserGroupMembership[]

  @@map("groups")
}

// User Group Membership
model UserGroupMembership {
  id        String   @id @default(uuid())
  userId    String
  groupId   String
  createdAt DateTime @default(now())

  user  User  @relation(fields: [userId], references: [id])
  group Group @relation(fields: [groupId], references: [id])

  @@unique([userId, groupId])
  @@map("user_group_membership")
}
```

### 2. Screen Schemes (jira-admin-service)

```prisma
// Screen Scheme
model ScreenScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())

  @@map("screen_schemes")
}

// Issue Type Screen Scheme
model IssueTypeScreenScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())

  mappings    IssueTypeScreenSchemeMapping[]

  @@map("issue_type_screen_schemes")
}

// Issue Type to Screen Scheme Mapping
model IssueTypeScreenSchemeMapping {
  id                        String   @id @default(uuid())
  issueTypeScreenSchemeId   String
  issueTypeId               String?
  screenSchemeId            String
  createdAt                 DateTime @default(now())

  issueTypeScreenScheme IssueTypeScreenScheme @relation(fields: [issueTypeScreenSchemeId], references: [id])
  screenScheme          ScreenScheme         @relation(fields: [screenSchemeId], references: [id])

  @@unique([issueTypeScreenSchemeId, issueTypeId])
  @@map("issue_type_screen_scheme_mappings")
}

// Field Configuration Scheme
model FieldConfigurationScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())

  @@map("field_configuration_schemes")
}

// Field Configuration
model FieldConfiguration {
  id          String   @id @default(uuid())
  name        String
  description String?
  createdAt   DateTime @default(now())

  items       FieldConfigurationItem[]

  @@map("field_configurations")
}

// Field Configuration Item
model FieldConfigurationItem {
  id                    String   @id @default(uuid())
  fieldConfigurationId  String
  fieldKey              String
  isShown               Boolean  @default(true)
  isRequired            Boolean  @default(false)
  isEditable            Boolean  @default(true)
  renderer              String?
  ordering              Int      @default(0)

  fieldConfiguration    FieldConfiguration @relation(fields: [fieldConfigurationId], references: [id])

  @@unique([fieldConfigurationId, fieldKey])
  @@map("field_configuration_items")
}
```

### 3. Notification Schemes (jira-admin-service)

```prisma
// Notification Scheme
model NotificationScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  events      NotificationSchemeEvent[]
  projects    ProjectNotificationScheme[]

  @@map("notification_schemes")
}

// Notification Event
model NotificationEvent {
  id          String   @id @default(uuid())
  eventKey    String   @unique
  name        String
  description String?
  createdAt   DateTime @default(now())

  @@map("notification_events")
}

// Notification Scheme Event (recipient config)
model NotificationSchemeEvent {
  id                   String   @id @default(uuid())
  notificationSchemeId String
  eventId              String
  notificationType     String   // 'USER', 'GROUP', 'PROJECT_ROLE', 'CURRENT_USER', 'REPORTER', 'ASSIGNEE'
  notifierId           String?
  createdAt            DateTime @default(now())

  notificationScheme NotificationScheme @relation(fields: [notificationSchemeId], references: [id])
  event            NotificationEvent  @relation(fields: [eventId], references: [id])

  @@unique([notificationSchemeId, eventId, notificationType, notifierId])
  @@map("notification_scheme_events")
}

// Project Notification Scheme
model ProjectNotificationScheme {
  id                   String   @id @default(uuid())
  projectId            String
  notificationSchemeId String
  createdAt           DateTime @default(now())

  project            Project             @relation(fields: [projectId], references: [id])
  notificationScheme NotificationScheme @relation(fields: [notificationSchemeId], references: [id])

  @@unique([projectId])
  @@map("project_notification_schemes")
}
```

### 4. Saved Filters (jira-search-service)

```prisma
// Saved Filter
model SavedFilter {
  id             String   @id @default(uuid())
  name           String
  description    String?
  jqlQuery       String
  ownerId        String
  isShareable    Boolean  @default(true)
  isFavorite     Boolean  @default(false)
  favoriteCount  Int      @default(0)
  filterColumns Json     @default("[]")
  viewFormat     String   @default("list")
  groupBy        String?
  sortColumns    Json     @default("[]")
  createdAt      DateTime @default(now())
  updatedAt      DateTime @updatedAt

  permissions FilterPermission[]
  favorites   FilterFavorite[]

  @@map("saved_filters")
}

// Filter Permission
model FilterPermission {
  id             String   @id @default(uuid())
  filterId       String
  permissionType String   // 'USER', 'GROUP', 'PROJECT', 'PROJECT_ROLE'
  permissionId   String?
  canEdit       Boolean  @default(false)
  createdAt      DateTime @default(now())

  filter         SavedFilter @relation(fields: [filterId], references: [id])

  @@unique([filterId, permissionType, permissionId])
  @@map("filter_permissions")
}

// Filter Favorite
model FilterFavorite {
  id        String   @id @default(uuid())
  filterId  String
  userId    String
  sequence  Int      @default(0)
  createdAt DateTime @default(now())

  filter SavedFilter @relation(fields: [filterId], references: [id])
  user   User        @relation(fields: [userId], references: [id])

  @@unique([filterId, userId])
  @@map("filter_favorites")
}
```

### 5. Sprint Snapshots (jira-plan-service)

```prisma
// Sprint Snapshot
model SprintSnapshot {
  id                  String   @id @default(uuid())
  sprintId            String
  snapshotType        String   // 'COMMITMENT', 'DAILY', 'CLOSURE'
  snapshotTime        DateTime @default(now())
  totalIssues         Int      @default(0)
  completedIssues     Int      @default(0)
  incompleteIssues    Int      @default(0)
  addedAfterStart     Int      @default(0)
  removedAfterStart   Int      @default(0)
  totalPoints         Decimal  @db.Decimal(10,2)
  completedPoints     Decimal  @db.Decimal(10,2)
  incompletePoints    Decimal  @db.Decimal(10,2)
  originalPoints      Decimal  @db.Decimal(10,2)
  idealRemainingPoints Decimal @db.Decimal(10,2)
  scopeChangePoints   Decimal  @db.Decimal(10,2)
  velocityTrend       Decimal  @db.Decimal(5,2)
  issueBreakdown      Json     @default("{}")
  createdAt          DateTime @default(now())

  sprint Sprint @relation(fields: [sprintId], references: [id])

  @@map("sprint_snapshots")
}

// Velocity History
model VelocityHistory {
  id               String    @id @default(uuid())
  boardId          String
  sprintId         String
  sprintName       String?
  sprintStartDate  Date?
  sprintEndDate    Date?
  sprintCompletedDate Date?
  plannedPoints    Decimal   @db.Decimal(10,2) @default(0)
  completedPoints  Decimal   @db.Decimal(10,2) @default(0)
  issueCount       Int       @default(0)
  velocityTrend   Decimal   @db.Decimal(5,2) @default(0)
  createdAt       DateTime   @default(now())

  board Board @relation(fields: [boardId], references: [id])
  sprint Sprint @relation(fields: [sprintId], references: [id])

  @@unique([boardId, sprintId])
  @@map("velocity_history")
}

// Burndown Data
model BurndownData {
  id              BigInt @id @default(autoincrement())
  sprintId        String
  boardId         String
  recordDate      Date
  dayNumber       Int
  totalPoints     Decimal @db.Decimal(10,2)
  remainingPoints Decimal @db.Decimal(10,2)
  idealRemainingPoints Decimal @db.Decimal(10,2)
  completedPoints Decimal @db.Decimal(10,2)
  issueCount     Int     @default(0)
  completedIssues Int    @default(0)
  addedIssues    Int     @default(0)
  removedIssues  Int     @default(0)
  createdAt      DateTime @default(now())

  sprint Sprint @relation(fields: [sprintId], references: [id])

  @@unique([sprintId, recordDate])
  @@map("burndown_data")
}

// Cumulative Flow Data
model CumulativeFlowData {
  id             BigInt @id @default(autoincrement())
  boardId        String
  sprintId       String?
  recordedAt     DateTime @default(now())
  statusName     String
  statusColor    String?
  issueCount     Int     @default(0)
  issuePoints    Decimal @db.Decimal(10,2) @default(0)
  recordDate     Date

  @@unique([boardId, sprintId, statusName, recordDate])
  @@map("cumulative_flow_data")
}
```

### 6. Epic Progress (jira-issue-service)

```prisma
// Epic (separate from Issue with epicId)
model Epic {
  id                   String   @id @default(uuid())
  name                 String
  summary              String?  @db.VarChar(500)
  description          String?
  color                String   @default("#0052CC")
  leadId               String?
  leadName             String?
  status               String   @default("OPEN")
  startDate            Date?
  endDate              Date?
  linkedIssueId        String?  // Links to actual Issue record
  totalStoryPoints     Decimal  @db.Decimal(10,2) @default(0)
  completedStoryPoints Decimal  @db.Decimal(10,2) @default(0)
  totalIssueCount      Int      @default(0)
  completedIssueCount  Int      @default(0)
  createdAt            DateTime @default(now())
  updatedAt            DateTime @updatedAt

  issues   EpicIssue[]
  progress EpicProgressHistory[]

  @@map("epics")
}

// Epic to Issue linking
model EpicIssue {
  id       String   @id @default(uuid())
  epicId   String
  issueId  String
  addedAt  DateTime @default(now())
  addedBy  String?

  epic  Epic  @relation(fields: [epicId], references: [id])
  issue Issue @relation(fields: [issueId], references: [id])

  @@unique([epicId, issueId])
  @@map("epic_issues")
}

// Epic Progress History
model EpicProgressHistory {
  id                BigInt @id @default(autoincrement())
  epicId            String
  recordDate        Date
  totalPoints       Decimal @db.Decimal(10,2) @default(0)
  completedPoints   Decimal @db.Decimal(10,2) @default(0)
  totalIssues       Int     @default(0)
  completedIssues   Int     @default(0)
  percentComplete   Decimal @db.Decimal(5,2) @default(0)
  createdAt         DateTime @default(now())

  epic Epic @relation(fields: [epicId], references: [id])

  @@unique([epicId, recordDate])
  @@map("epic_progress_history")
}
```

### 7. Optimistic Locking (jira-issue-service)

```prisma
// Add to existing Issue model
model Issue {
  // ... existing fields ...

  // New fields for optimistic locking
  version              BigInt   @default(0)
  lastModifiedVersion BigInt   @default(0)

  @@map("issues")
}
```

## Service-Specific Schema Organization

### jira-admin-service/prisma/schema.prisma
```prisma
generator client {
  provider = "prisma-client-js"
  output   = "../node_modules/@jira-platform/admin-service/prisma-client"
}

datasource db {
  provider = "postgresql"
  url      = env("ADMIN_SERVICE_DATABASE_URL")
}

model PermissionScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  grants   PermissionSchemeGrant[]
  projects ProjectPermissionScheme[]

  @@map("permission_schemes")
}

model Permission {
  id             String @id @default(uuid())
  permissionKey  String @unique
  permissionType String
  description    String?

  grants          PermissionSchemeGrant[]
  rolePermissions RolePermission[]

  @@map("permissions")
}

model PermissionSchemeGrant {
  id                 String   @id @default(uuid())
  permissionSchemeId String
  permissionId       String
  holderType         String
  holderId           String?
  createdAt          DateTime @default(now())

  permissionScheme PermissionScheme @relation(fields: [permissionSchemeId], references: [id])
  permission       Permission       @relation(fields: [permissionId], references: [id])

  @@unique([permissionSchemeId, permissionId, holderType, holderId])
  @@map("permission_scheme_grants")
}

model ProjectRole {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  permissions RolePermission[]
  actors      ProjectRoleActor[]

  @@map("project_roles")
}

model RolePermission {
  id            String   @id @default(uuid())
  projectRoleId String
  permissionId  String
  createdAt     DateTime @default(now())

  projectRole ProjectRole @relation(fields: [projectRoleId], references: [id])
  permission Permission   @relation(fields: [permissionId], references: [id])

  @@unique([projectRoleId, permissionId])
  @@map("role_permissions")
}

model ProjectRoleActor {
  id            String   @id @default(uuid())
  projectId     String
  projectRoleId String
  holderType    String
  holderId      String
  createdAt     DateTime @default(now())

  project     Project     @relation(fields: [projectId], references: [id])
  projectRole ProjectRole @relation(fields: [projectRoleId], references: [id])

  @@unique([projectId, projectRoleId, holderType, holderId])
  @@map("project_role_actors")
}

model Group {
  id          String   @id @default(uuid())
  groupName   String   @unique
  description String?
  createdAt   DateTime @default(now())

  memberships UserGroupMembership[]

  @@map("groups")
}

model UserGroupMembership {
  id        String   @id @default(uuid())
  userId    String
  groupId   String
  createdAt DateTime @default(now())

  user  User  @relation(fields: [userId], references: [id])
  group Group @relation(fields: [groupId], references: [id])

  @@unique([userId, groupId])
  @@map("user_group_membership")
}

// Screen Schemas
model ScreenScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())

  @@map("screen_schemes")
}

model IssueTypeScreenScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())

  mappings IssueTypeScreenSchemeMapping[]

  @@map("issue_type_screen_schemes")
}

model IssueTypeScreenSchemeMapping {
  id                      String   @id @default(uuid())
  issueTypeScreenSchemeId String
  issueTypeId             String?
  screenSchemeId          String
  createdAt               DateTime @default(now())

  issueTypeScreenScheme IssueTypeScreenScheme @relation(fields: [issueTypeScreenSchemeId], references: [id])
  screenScheme          ScreenScheme          @relation(fields: [screenSchemeId], references: [id])

  @@unique([issueTypeScreenSchemeId, issueTypeId])
  @@map("issue_type_screen_scheme_mappings")
}

// Field Configuration
model FieldConfigurationScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())

  @@map("field_configuration_schemes")
}

model FieldConfiguration {
  id          String   @id @default(uuid())
  name        String
  description String?
  createdAt   DateTime @default(now())

  items FieldConfigurationItem[]

  @@map("field_configurations")
}

model FieldConfigurationItem {
  id                   String   @id @default(uuid())
  fieldConfigurationId String
  fieldKey             String
  isShown              Boolean  @default(true)
  isRequired           Boolean  @default(false)
  isEditable           Boolean  @default(true)
  renderer             String?
  ordering             Int      @default(0)

  fieldConfiguration FieldConfiguration @relation(fields: [fieldConfigurationId], references: [id])

  @@unique([fieldConfigurationId, fieldKey])
  @@map("field_configuration_items")
}

// Notification Schemas
model NotificationScheme {
  id          String   @id @default(uuid())
  name        String
  description String?
  isDefault   Boolean  @default(false)
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  events   NotificationSchemeEvent[]
  projects ProjectNotificationScheme[]

  @@map("notification_schemes")
}

model NotificationEvent {
  id          String @id @default(uuid())
  eventKey    String @unique
  name        String
  description String?

  @@map("notification_events")
}

model NotificationSchemeEvent {
  id                   String   @id @default(uuid())
  notificationSchemeId String
  eventId              String
  notificationType     String
  notifierId           String?
  createdAt            DateTime @default(now())

  notificationScheme NotificationScheme @relation(fields: [notificationSchemeId], references: [id])
  event             NotificationEvent  @relation(fields: [eventId], references: [id])

  @@unique([notificationSchemeId, eventId, notificationType, notifierId])
  @@map("notification_scheme_events")
}

model ProjectNotificationScheme {
  id                   String   @id @default(uuid())
  projectId            String
  notificationSchemeId String
  createdAt           DateTime @default(now())

  notificationScheme NotificationScheme @relation(fields: [notificationSchemeId], references: [id])

  @@unique([projectId])
  @@map("project_notification_schemes")
}
```

## Usage Examples

### Check User Permissions
```typescript
async function hasPermission(userId: string, projectId: string, permissionKey: string): Promise<boolean> {
  // 1. Get user's groups
  const memberships = await prisma.userGroupMembership.findMany({
    where: { userId },
    include: { group: true }
  });
  const groupIds = memberships.map(m => m.groupId);

  // 2. Get project role actors for this project
  const roleActors = await prisma.projectRoleActor.findMany({
    where: { projectId }
  });

  // 3. Check if any role has the permission
  for (const actor of roleActors) {
    const matches = 
      (actor.holderType === 'USER' && actor.holderId === userId) ||
      (actor.holderType === 'GROUP' && groupIds.includes(actor.holderId));
    
    if (matches) {
      const rolePermission = await prisma.rolePermission.findFirst({
        where: {
          projectRoleId: actor.projectRoleId,
          permission: { permissionKey }
        }
      });
      if (rolePermission) return true;
    }
  }

  return false;
}
```

### Record Sprint Snapshot
```typescript
async function recordSprintStartSnapshot(sprintId: string) {
  const issues = await prisma.issue.findMany({
    where: { sprintId },
    select: { id: true, storyPoints: true }
  });

  const totalPoints = issues.reduce((sum, i) => sum + (i.storyPoints || 0), 0);

  await prisma.sprintSnapshot.create({
    data: {
      sprintId,
      snapshotType: 'COMMITMENT',
      totalIssues: issues.length,
      totalPoints,
      originalPoints: totalPoints,
      incompletePoints: totalPoints
    }
  });
}
```

### Get Epic Progress
```typescript
async function calculateEpicProgress(epicId: string) {
  const epicIssues = await prisma.epicIssue.findMany({
    where: { epicId },
    include: { issue: true }
  });

  const totalPoints = epicIssues.reduce((sum, ei) => sum + (ei.issue.storyPoints || 0), 0);
  const doneStatus = await getDoneStatusId();
  const completedPoints = epicIssues
    .filter(ei => ei.issue.statusId === doneStatus)
    .reduce((sum, ei) => sum + (ei.issue.storyPoints || 0), 0);

  await prisma.epic.update({
    where: { id: epicId },
    data: {
      totalStoryPoints: totalPoints,
      completedStoryPoints: completedPoints,
      totalIssueCount: epicIssues.length,
      completedIssueCount: epicIssues.filter(ei => ei.issue.statusId === doneStatus).length
    }
  });
}
```