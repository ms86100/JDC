import type { languages } from 'monaco-editor';

type CompletionItem = languages.CompletionItem;
type CompletionItemKind = languages.CompletionItemKind;

interface ApiMethod {
  label: string;
  detail: string;
  documentation: string;
  insertText: string;
  kind: 'Method' | 'Property' | 'Module';
}

const APIS: Record<string, ApiMethod[]> = {
  'jdc.issue': [
    { label: 'getCurrentIssue', detail: '(): Map', documentation: 'Get the current issue data from the workflow context', insertText: 'getCurrentIssue()', kind: 'Method' },
    { label: 'getIssue', detail: '(idOrKey: string): Map', documentation: 'Get issue by ID or key', insertText: 'getIssue(${1:idOrKey})', kind: 'Method' },
    { label: 'getFieldValue', detail: '(fieldName: string): any', documentation: 'Get a field value from the current issue', insertText: 'getFieldValue(${1:fieldName})', kind: 'Method' },
    { label: 'setFieldValue', detail: '(fieldName: string, value: any): boolean', documentation: 'Set a field value on the current issue', insertText: 'setFieldValue(${1:fieldName}, ${2:value})', kind: 'Method' },
    { label: 'createIssue', detail: '(projectId, issueTypeId, summary, fields?): Map', documentation: 'Create a new issue. Returns the created issue data', insertText: 'createIssue(${1:projectId}, ${2:issueTypeId}, ${3:summary}, ${4:{}})', kind: 'Method' },
    { label: 'cloneIssue', detail: '(idOrKey: string): Map', documentation: 'Clone an existing issue', insertText: 'cloneIssue(${1:idOrKey})', kind: 'Method' },
    { label: 'moveIssue', detail: '(issueId, targetProjectId): boolean', documentation: 'Move issue to another project', insertText: 'moveIssue(${1:issueId}, ${2:targetProjectId})', kind: 'Method' },
    { label: 'deleteIssue', detail: '(issueId: string): boolean', documentation: 'Delete an issue permanently', insertText: 'deleteIssue(${1:issueId})', kind: 'Method' },
    { label: 'transitionIssue', detail: '(issueId, transitionId): boolean', documentation: 'Execute a workflow transition on an issue', insertText: 'transitionIssue(${1:issueId}, ${2:transitionId})', kind: 'Method' },
    { label: 'addComment', detail: '(text: string): Map', documentation: 'Add a comment to the current issue', insertText: 'addComment(${1:text})', kind: 'Method' },
    { label: 'getComments', detail: '(): List<Map>', documentation: 'Get all comments on the current issue', insertText: 'getComments()', kind: 'Method' },
    { label: 'deleteComment', detail: '(commentId: string): boolean', documentation: 'Delete a comment by ID', insertText: 'deleteComment(${1:commentId})', kind: 'Method' },
    { label: 'updateComment', detail: '(commentId, newText): boolean', documentation: 'Edit an existing comment', insertText: 'updateComment(${1:commentId}, ${2:newText})', kind: 'Method' },
    { label: 'getLastComment', detail: '(): Map', documentation: 'Get the most recent comment', insertText: 'getLastComment()', kind: 'Method' },
    { label: 'getHistory', detail: '(): List<Map>', documentation: 'Get change history for the current issue', insertText: 'getHistory()', kind: 'Method' },
    { label: 'getWatchers', detail: '(): List<Map>', documentation: 'Get watchers of the current issue', insertText: 'getWatchers()', kind: 'Method' },
    { label: 'addWatcher', detail: '(userId: string): boolean', documentation: 'Add a watcher to the current issue', insertText: 'addWatcher(${1:userId})', kind: 'Method' },
    { label: 'removeWatcher', detail: '(userId: string): boolean', documentation: 'Remove a watcher', insertText: 'removeWatcher(${1:userId})', kind: 'Method' },
    { label: 'link', detail: '(targetKey, linkType): boolean', documentation: 'Create a link between current issue and target', insertText: 'link(${1:targetKey}, ${2:linkType})', kind: 'Method' },
    { label: 'unlinkIssue', detail: '(sourceId, targetId): boolean', documentation: 'Remove a link between two issues', insertText: 'unlinkIssue(${1:sourceId}, ${2:targetId})', kind: 'Method' },
    { label: 'getLinkedIssues', detail: '(): List<Map>', documentation: 'Get all linked issues', insertText: 'getLinkedIssues()', kind: 'Method' },
    { label: 'addLabel', detail: '(label: string): boolean', documentation: 'Add a label to the current issue', insertText: 'addLabel(${1:label})', kind: 'Method' },
    { label: 'removeLabel', detail: '(label: string): boolean', documentation: 'Remove a label', insertText: 'removeLabel(${1:label})', kind: 'Method' },
    { label: 'getLabels', detail: '(): List<string>', documentation: 'Get all labels on the current issue', insertText: 'getLabels()', kind: 'Method' },
    { label: 'addWorklog', detail: '(timeSpent, comment): Map', documentation: 'Add a worklog entry', insertText: 'addWorklog(${1:timeSpent}, ${2:comment})', kind: 'Method' },
    { label: 'getWorklogs', detail: '(): List<Map>', documentation: 'Get all worklogs', insertText: 'getWorklogs()', kind: 'Method' },
    { label: 'getSubtasks', detail: '(): List<Map>', documentation: 'Get all subtasks of the current issue', insertText: 'getSubtasks()', kind: 'Method' },
    { label: 'getAttachmentCount', detail: '(): number', documentation: 'Count attachments on the current issue', insertText: 'getAttachmentCount()', kind: 'Method' },
    { label: 'getAttachments', detail: '(): List<Map>', documentation: 'Get all attachment metadata', insertText: 'getAttachments()', kind: 'Method' },
    { label: 'deleteAttachment', detail: '(attachmentId: string): boolean', documentation: 'Delete an attachment', insertText: 'deleteAttachment(${1:attachmentId})', kind: 'Method' },
    { label: 'copyAttachments', detail: '(targetIssueId: string): boolean', documentation: 'Copy attachments to another issue', insertText: 'copyAttachments(${1:targetIssueId})', kind: 'Method' },
    { label: 'addVote', detail: '(): boolean', documentation: 'Vote for the current issue', insertText: 'addVote()', kind: 'Method' },
    { label: 'removeVote', detail: '(): boolean', documentation: 'Remove your vote', insertText: 'removeVote()', kind: 'Method' },
    { label: 'hasField', detail: '(fieldName: string): boolean', documentation: 'Check if the issue has a specific field', insertText: 'hasField(${1:fieldName})', kind: 'Method' },
    { label: 'clearField', detail: '(fieldName: string): boolean', documentation: 'Clear a field value (set to null)', insertText: 'clearField(${1:fieldName})', kind: 'Method' },
    { label: 'getSecurityLevel', detail: '(): string', documentation: 'Get the issue security level', insertText: 'getSecurityLevel()', kind: 'Method' },
    { label: 'setSecurityLevel', detail: '(issueId, levelId): boolean', documentation: 'Set the issue security level', insertText: 'setSecurityLevel(${1:issueId}, ${2:levelId})', kind: 'Method' },
  ],
  'jdc.project': [
    { label: 'getCurrentProject', detail: '(): Map', documentation: 'Get the current project from context', insertText: 'getCurrentProject()', kind: 'Method' },
    { label: 'getProject', detail: '(id: string): Map', documentation: 'Get project by ID', insertText: 'getProject(${1:projectId})', kind: 'Method' },
    { label: 'getProjectByKey', detail: '(key: string): Map', documentation: 'Get project by key', insertText: 'getProjectByKey(${1:key})', kind: 'Method' },
    { label: 'getVersions', detail: '(projectId: string): List<Map>', documentation: 'Get project versions', insertText: 'getVersions(${1:projectId})', kind: 'Method' },
    { label: 'createVersion', detail: '(projectId, name, releaseDate): Map', documentation: 'Create a new version', insertText: 'createVersion(${1:projectId}, ${2:name}, ${3:releaseDate})', kind: 'Method' },
    { label: 'releaseVersion', detail: '(versionId: string): boolean', documentation: 'Release a version', insertText: 'releaseVersion(${1:versionId})', kind: 'Method' },
    { label: 'archiveVersion', detail: '(versionId: string): boolean', documentation: 'Archive a version', insertText: 'archiveVersion(${1:versionId})', kind: 'Method' },
    { label: 'deleteVersion', detail: '(versionId: string): boolean', documentation: 'Delete a version', insertText: 'deleteVersion(${1:versionId})', kind: 'Method' },
    { label: 'getComponents', detail: '(projectId: string): List<Map>', documentation: 'Get project components', insertText: 'getComponents(${1:projectId})', kind: 'Method' },
    { label: 'createComponent', detail: '(projectId, name, leadId): Map', documentation: 'Create a component', insertText: 'createComponent(${1:projectId}, ${2:name}, ${3:leadId})', kind: 'Method' },
    { label: 'deleteComponent', detail: '(componentId: string): boolean', documentation: 'Delete a component', insertText: 'deleteComponent(${1:componentId})', kind: 'Method' },
    { label: 'getIssueTypes', detail: '(): List<Map>', documentation: 'Get issue types for the project', insertText: 'getIssueTypes()', kind: 'Method' },
    { label: 'getMembers', detail: '(projectId: string): List<Map>', documentation: 'Get project members', insertText: 'getMembers(${1:projectId})', kind: 'Method' },
    { label: 'getAllProjects', detail: '(query?: string): List<Map>', documentation: 'Search all projects', insertText: 'getAllProjects(${1:query})', kind: 'Method' },
    { label: 'getProjectRoles', detail: '(projectId: string): List<Map>', documentation: 'Get project roles', insertText: 'getProjectRoles(${1:projectId})', kind: 'Method' },
  ],
  'jdc.user': [
    { label: 'getCurrentUser', detail: '(): Map', documentation: 'Get the current user from context', insertText: 'getCurrentUser()', kind: 'Method' },
    { label: 'getUser', detail: '(userId: string): Map', documentation: 'Get user by ID', insertText: 'getUser(${1:userId})', kind: 'Method' },
    { label: 'isInGroup', detail: '(groupName: string): boolean', documentation: 'Check if current user is in a group', insertText: 'isInGroup(${1:groupName})', kind: 'Method' },
    { label: 'hasPermission', detail: '(permission: string): boolean', documentation: 'Check if current user has a permission', insertText: 'hasPermission(${1:permission})', kind: 'Method' },
    { label: 'getUserGroups', detail: '(): List<string>', documentation: 'Get groups the current user belongs to', insertText: 'getUserGroups()', kind: 'Method' },
    { label: 'addUserToGroup', detail: '(userId, groupName): boolean', documentation: 'Add a user to a group', insertText: 'addUserToGroup(${1:userId}, ${2:groupName})', kind: 'Method' },
    { label: 'removeUserFromGroup', detail: '(userId, groupName): boolean', documentation: 'Remove a user from a group', insertText: 'removeUserFromGroup(${1:userId}, ${2:groupName})', kind: 'Method' },
    { label: 'isAdmin', detail: '(userId: string): boolean', documentation: 'Check if a user is an administrator', insertText: 'isAdmin(${1:userId})', kind: 'Method' },
    { label: 'getUserByEmail', detail: '(email: string): Map', documentation: 'Find user by email address', insertText: 'getUserByEmail(${1:email})', kind: 'Method' },
    { label: 'getAllUsers', detail: '(query, limit): List<Map>', documentation: 'Search users', insertText: 'getAllUsers(${1:query}, ${2:50})', kind: 'Method' },
  ],
  'jdc.search': [
    { label: 'jql', detail: '(query, maxResults?): List<Map>', documentation: 'Execute a JQL query (max 500 results)', insertText: 'jql(${1:query}, ${2:100})', kind: 'Method' },
    { label: 'findIssues', detail: '(projectKey, statusName): List<Map>', documentation: 'Find issues by project and status', insertText: 'findIssues(${1:projectKey}, ${2:statusName})', kind: 'Method' },
    { label: 'batch', detail: '(jqlQuery, batchSize): number', documentation: 'Count results of a JQL query', insertText: 'batch(${1:jqlQuery}, ${2:50})', kind: 'Method' },
  ],
  'jdc.workflow': [
    { label: 'getCurrentTransition', detail: '(): Map', documentation: 'Get current transition context', insertText: 'getCurrentTransition()', kind: 'Method' },
    { label: 'getAllStatuses', detail: '(): List<Map>', documentation: 'Get all workflow statuses', insertText: 'getAllStatuses()', kind: 'Method' },
    { label: 'getAvailableActions', detail: '(issueId): List<Map>', documentation: 'Get available transitions for an issue', insertText: 'getAvailableActions(${1:issueId})', kind: 'Method' },
    { label: 'getWorkflowName', detail: '(issueId): string', documentation: 'Get the workflow name for an issue', insertText: 'getWorkflowName(${1:issueId})', kind: 'Method' },
  ],
  'jdc.log': [
    { label: 'info', detail: '(...args): void', documentation: 'Log an info message', insertText: 'info(${1:message})', kind: 'Method' },
    { label: 'warn', detail: '(...args): void', documentation: 'Log a warning', insertText: 'warn(${1:message})', kind: 'Method' },
    { label: 'error', detail: '(...args): void', documentation: 'Log an error', insertText: 'error(${1:message})', kind: 'Method' },
    { label: 'debug', detail: '(...args): void', documentation: 'Log a debug message', insertText: 'debug(${1:message})', kind: 'Method' },
  ],
  'http': [
    { label: 'get', detail: '(url, headers?): Map', documentation: 'HTTP GET request', insertText: 'get(${1:url}, ${2:{}})', kind: 'Method' },
    { label: 'post', detail: '(url, body, headers?): Map', documentation: 'HTTP POST request', insertText: 'post(${1:url}, ${2:body}, ${3:{}})', kind: 'Method' },
    { label: 'put', detail: '(url, body, headers?): Map', documentation: 'HTTP PUT request', insertText: 'put(${1:url}, ${2:body}, ${3:{}})', kind: 'Method' },
    { label: 'delete', detail: '(url, headers?): Map', documentation: 'HTTP DELETE request', insertText: 'delete(${1:url}, ${2:{}})', kind: 'Method' },
    { label: 'patch', detail: '(url, body, headers?): Map', documentation: 'HTTP PATCH request', insertText: 'patch(${1:url}, ${2:body}, ${3:{}})', kind: 'Method' },
  ],
  'sql': [
    { label: 'query', detail: '(dsName, sql, params?): List<Map>', documentation: 'Execute a SELECT query', insertText: 'query(${1:dsName}, ${2:sql}, ${3:[]})', kind: 'Method' },
    { label: 'update', detail: '(dsName, sql, params?): number', documentation: 'Execute INSERT/UPDATE/DELETE', insertText: 'update(${1:dsName}, ${2:sql}, ${3:[]})', kind: 'Method' },
    { label: 'getDataSources', detail: '(): List<string>', documentation: 'List available datasource names', insertText: 'getDataSources()', kind: 'Method' },
  ],
  'vars': [
    { label: 'get', detail: '(key: string): string', documentation: 'Get a global persistent variable', insertText: 'get(${1:key})', kind: 'Method' },
    { label: 'set', detail: '(key, value): void', documentation: 'Set a global persistent variable', insertText: 'set(${1:key}, ${2:value})', kind: 'Method' },
    { label: 'remove', detail: '(key: string): void', documentation: 'Remove a global persistent variable', insertText: 'remove(${1:key})', kind: 'Method' },
    { label: 'getForIssue', detail: '(key: string): string', documentation: 'Get issue-scoped variable', insertText: 'getForIssue(${1:key})', kind: 'Method' },
    { label: 'setForIssue', detail: '(key, value): void', documentation: 'Set issue-scoped variable', insertText: 'setForIssue(${1:key}, ${2:value})', kind: 'Method' },
    { label: 'getForProject', detail: '(key: string): string', documentation: 'Get project-scoped variable', insertText: 'getForProject(${1:key})', kind: 'Method' },
    { label: 'setForProject', detail: '(key, value): void', documentation: 'Set project-scoped variable', insertText: 'setForProject(${1:key}, ${2:value})', kind: 'Method' },
  ],
  'email': [
    { label: 'sendEmail', detail: '(to, subject, htmlBody): boolean', documentation: 'Send an HTML email', insertText: 'sendEmail(${1:to}, ${2:subject}, ${3:htmlBody})', kind: 'Method' },
    { label: 'sendToUser', detail: '(userId, subject, message): boolean', documentation: 'Send notification to a user by ID', insertText: 'sendToUser(${1:userId}, ${2:subject}, ${3:message})', kind: 'Method' },
    { label: 'sendEmailWithCc', detail: '(to, cc, bcc, subject, body): boolean', documentation: 'Send email with CC/BCC', insertText: 'sendEmailWithCc(${1:to}, ${2:cc}, ${3:bcc}, ${4:subject}, ${5:body})', kind: 'Method' },
  ],
  'xml': [
    { label: 'parse', detail: '(xmlString: string): Map', documentation: 'Parse XML string to object', insertText: 'parse(${1:xmlString})', kind: 'Method' },
    { label: 'toXml', detail: '(rootName, data): string', documentation: 'Convert object to XML string', insertText: 'toXml(${1:rootName}, ${2:data})', kind: 'Method' },
    { label: 'xpath', detail: '(xmlString, expression): string', documentation: 'Evaluate XPath expression', insertText: 'xpath(${1:xmlString}, ${2:expression})', kind: 'Method' },
  ],
  'ldap': [
    { label: 'search', detail: '(query: string): List<Map>', documentation: 'Search the user directory', insertText: 'search(${1:query})', kind: 'Method' },
    { label: 'getUser', detail: '(userId: string): Map', documentation: 'Get user from directory', insertText: 'getUser(${1:userId})', kind: 'Method' },
    { label: 'getGroupMembers', detail: '(groupName: string): List<Map>', documentation: 'Get members of a group', insertText: 'getGroupMembers(${1:groupName})', kind: 'Method' },
    { label: 'getGroups', detail: '(query: string): List<Map>', documentation: 'Search groups', insertText: 'getGroups(${1:query})', kind: 'Method' },
  ],
  'confluence': [
    { label: 'getPage', detail: '(pageId: string): Map', documentation: 'Get a Confluence page', insertText: 'getPage(${1:pageId})', kind: 'Method' },
    { label: 'createPage', detail: '(spaceKey, title, htmlBody, parentId?): Map', documentation: 'Create a Confluence page', insertText: 'createPage(${1:spaceKey}, ${2:title}, ${3:htmlBody}, ${4:parentId})', kind: 'Method' },
    { label: 'updatePage', detail: '(pageId, title, htmlBody, version): Map', documentation: 'Update a Confluence page', insertText: 'updatePage(${1:pageId}, ${2:title}, ${3:htmlBody}, ${4:version})', kind: 'Method' },
    { label: 'search', detail: '(cql, limit): List<Map>', documentation: 'Search Confluence with CQL', insertText: 'search(${1:cql}, ${2:25})', kind: 'Method' },
  ],
  'sprint': [
    { label: 'getSprint', detail: '(sprintId: string): Map', documentation: 'Get sprint by ID', insertText: 'getSprint(${1:sprintId})', kind: 'Method' },
    { label: 'getActiveSprint', detail: '(boardId: string): Map', documentation: 'Get the active sprint for a board', insertText: 'getActiveSprint(${1:boardId})', kind: 'Method' },
    { label: 'getAllSprints', detail: '(boardId: string): List<Map>', documentation: 'Get all sprints for a board', insertText: 'getAllSprints(${1:boardId})', kind: 'Method' },
    { label: 'moveToSprint', detail: '(issueId, sprintId): boolean', documentation: 'Move an issue to a sprint', insertText: 'moveToSprint(${1:issueId}, ${2:sprintId})', kind: 'Method' },
    { label: 'moveToBacklog', detail: '(issueId: string): boolean', documentation: 'Move an issue to the backlog', insertText: 'moveToBacklog(${1:issueId})', kind: 'Method' },
    { label: 'getSprintIssues', detail: '(sprintId: string): List<Map>', documentation: 'Get all issues in a sprint', insertText: 'getSprintIssues(${1:sprintId})', kind: 'Method' },
    { label: 'createSprint', detail: '(boardId, name, startDate, endDate): Map', documentation: 'Create a new sprint', insertText: 'createSprint(${1:boardId}, ${2:name}, ${3:startDate}, ${4:endDate})', kind: 'Method' },
    { label: 'closeSprint', detail: '(sprintId: string): boolean', documentation: 'Close a sprint', insertText: 'closeSprint(${1:sprintId})', kind: 'Method' },
    { label: 'getEpic', detail: '(epicId: string): Map', documentation: 'Get epic details (epics are issues)', insertText: 'getEpic(${1:epicId})', kind: 'Method' },
  ],
  'webhook': [
    { label: 'setResponseCode', detail: '(code: number): void', documentation: 'Set HTTP response code (for execute-by-key scripts)', insertText: 'setResponseCode(${1:200})', kind: 'Method' },
    { label: 'setResponseBody', detail: '(body: any): void', documentation: 'Set HTTP response body', insertText: 'setResponseBody(${1:body})', kind: 'Method' },
    { label: 'setResponseHeader', detail: '(name, value): void', documentation: 'Set an HTTP response header', insertText: 'setResponseHeader(${1:name}, ${2:value})', kind: 'Method' },
  ],
  'env': [
    { label: 'get', detail: '(key: string): string', documentation: 'Get a whitelisted environment variable', insertText: 'get(${1:key})', kind: 'Method' },
  ],
  'file': [
    { label: 'writeFile', detail: '(path, content): boolean', documentation: 'Write content to in-memory file (max 10MB total)', insertText: 'writeFile(${1:path}, ${2:content})', kind: 'Method' },
    { label: 'readFile', detail: '(path: string): string', documentation: 'Read content from in-memory file', insertText: 'readFile(${1:path})', kind: 'Method' },
    { label: 'fileExists', detail: '(path: string): boolean', documentation: 'Check if in-memory file exists', insertText: 'fileExists(${1:path})', kind: 'Method' },
    { label: 'deleteFile', detail: '(path: string): boolean', documentation: 'Delete an in-memory file', insertText: 'deleteFile(${1:path})', kind: 'Method' },
    { label: 'listFiles', detail: '(prefix?: string): string[]', documentation: 'List in-memory files', insertText: 'listFiles(${1:prefix})', kind: 'Method' },
    { label: 'getFileSize', detail: '(path: string): number', documentation: 'Get file size in bytes', insertText: 'getFileSize(${1:path})', kind: 'Method' },
    { label: 'appendToFile', detail: '(path, content): boolean', documentation: 'Append content to in-memory file', insertText: 'appendToFile(${1:path}, ${2:content})', kind: 'Method' },
  ],
  'test': [
    { label: 'assertTrue', detail: '(condition, message): void', documentation: 'Assert condition is true', insertText: 'assertTrue(${1:condition}, ${2:message})', kind: 'Method' },
    { label: 'assertEquals', detail: '(expected, actual, message?): void', documentation: 'Assert two values are equal', insertText: 'assertEquals(${1:expected}, ${2:actual}, ${3:message})', kind: 'Method' },
    { label: 'assertNotNull', detail: '(value, message): void', documentation: 'Assert value is not null', insertText: 'assertNotNull(${1:value}, ${2:message})', kind: 'Method' },
    { label: 'assertNull', detail: '(value, message): void', documentation: 'Assert value is null', insertText: 'assertNull(${1:value}, ${2:message})', kind: 'Method' },
    { label: 'assertContains', detail: '(haystack, needle, message): void', documentation: 'Assert string contains substring', insertText: 'assertContains(${1:haystack}, ${2:needle}, ${3:message})', kind: 'Method' },
    { label: 'fail', detail: '(message: string): void', documentation: 'Explicitly fail the test', insertText: 'fail(${1:message})', kind: 'Method' },
    { label: 'allPassed', detail: '(): boolean', documentation: 'Check if all assertions passed', insertText: 'allPassed()', kind: 'Method' },
    { label: 'getPassed', detail: '(): number', documentation: 'Get count of passed assertions', insertText: 'getPassed()', kind: 'Method' },
    { label: 'getFailed', detail: '(): number', documentation: 'Get count of failed assertions', insertText: 'getFailed()', kind: 'Method' },
  ],
};

const TOP_LEVEL_MODULES = [
  { label: 'jdc', detail: 'JDC Platform API', documentation: 'Access issue, project, user, workflow, search, and log APIs', kind: 'Module' as const },
  { label: 'http', detail: 'HTTP Client', documentation: 'Make HTTP requests (GET, POST, PUT, DELETE, PATCH)', kind: 'Module' as const },
  { label: 'sql', detail: 'SQL Database', documentation: 'Query and update databases', kind: 'Module' as const },
  { label: 'vars', detail: 'Persistent Variables', documentation: 'Cross-script key-value storage (global, project, issue scoped)', kind: 'Module' as const },
  { label: 'email', detail: 'Email', documentation: 'Send emails and notifications', kind: 'Module' as const },
  { label: 'xml', detail: 'XML Parser', documentation: 'Parse and create XML documents', kind: 'Module' as const },
  { label: 'ldap', detail: 'User Directory', documentation: 'Query users and groups from the directory', kind: 'Module' as const },
  { label: 'confluence', detail: 'Confluence', documentation: 'Manage Confluence pages', kind: 'Module' as const },
  { label: 'sprint', detail: 'Sprint/Agile', documentation: 'Manage sprints, boards, and backlogs', kind: 'Module' as const },
  { label: 'webhook', detail: 'Webhook Response', documentation: 'Control HTTP response for webhook-triggered scripts', kind: 'Module' as const },
  { label: 'env', detail: 'Environment', documentation: 'Access whitelisted environment variables', kind: 'Module' as const },
  { label: 'file', detail: 'File I/O', documentation: 'In-memory file system (read, write, list — 10MB limit, per-execution)', kind: 'Module' as const },
  { label: 'test', detail: 'Testing', documentation: 'Assertion framework (assertTrue, assertEquals, assertNotNull, etc.)', kind: 'Module' as const },
  { label: 'console', detail: 'Console', documentation: 'Log output (log, info, warn, error, debug, table, dir, trace)', kind: 'Module' as const },
];

const JDC_SUB_MODULES = [
  { label: 'issue', detail: 'Issue API', documentation: 'CRUD operations on issues, comments, attachments, worklogs', kind: 'Module' as const },
  { label: 'project', detail: 'Project API', documentation: 'Project, version, and component management', kind: 'Module' as const },
  { label: 'user', detail: 'User API', documentation: 'User and group management', kind: 'Module' as const },
  { label: 'search', detail: 'Search API', documentation: 'JQL queries and issue search', kind: 'Module' as const },
  { label: 'workflow', detail: 'Workflow API', documentation: 'Transitions and status management', kind: 'Module' as const },
  { label: 'log', detail: 'Logging API', documentation: 'Server-side logging (info, warn, error, debug)', kind: 'Module' as const },
];

const CONTEXT_VARIABLES = [
  { label: 'issueId', detail: 'string', documentation: 'ID of the current issue', kind: 'Property' as const },
  { label: 'projectId', detail: 'string', documentation: 'ID of the current project', kind: 'Property' as const },
  { label: 'userId', detail: 'string', documentation: 'ID of the current user', kind: 'Property' as const },
  { label: 'issueTypeId', detail: 'string', documentation: 'ID of the issue type', kind: 'Property' as const },
  { label: 'currentStatusId', detail: 'string', documentation: 'Current status ID of the issue', kind: 'Property' as const },
  { label: 'transitionId', detail: 'string', documentation: 'ID of the current transition', kind: 'Property' as const },
  { label: 'transitionName', detail: 'string', documentation: 'Name of the current transition', kind: 'Property' as const },
  { label: 'fromStatusId', detail: 'string', documentation: 'Source status ID', kind: 'Property' as const },
  { label: 'toStatusId', detail: 'string', documentation: 'Target status ID', kind: 'Property' as const },
  { label: 'comment', detail: 'string', documentation: 'Comment text from transition screen', kind: 'Property' as const },
  { label: 'resolutionId', detail: 'string', documentation: 'Resolution ID', kind: 'Property' as const },
  { label: 'screenInput', detail: 'Map', documentation: 'All screen input fields from the transition', kind: 'Property' as const },
  { label: 'issueData', detail: 'Map', documentation: 'Full issue data object', kind: 'Property' as const },
  { label: 'userData', detail: 'Map', documentation: 'Current user data object', kind: 'Property' as const },
];

export function registerJdcCompletionProvider(monaco: typeof import('monaco-editor')) {
  const CompletionItemKind = monaco.languages.CompletionItemKind;

  function toKind(kind: string): CompletionItemKind {
    switch (kind) {
      case 'Method': return CompletionItemKind.Method;
      case 'Property': return CompletionItemKind.Property;
      case 'Module': return CompletionItemKind.Module;
      default: return CompletionItemKind.Function;
    }
  }

  return monaco.languages.registerCompletionItemProvider('javascript', {
    triggerCharacters: ['.'],
    provideCompletionItems(model, position) {
      const textUntilPosition = model.getValueInRange({
        startLineNumber: position.lineNumber,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column,
      });

      const word = model.getWordUntilPosition(position);
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      };

      const suggestions: CompletionItem[] = [];

      const dotMatch = textUntilPosition.match(/(\w+(?:\.\w+)*)\.$/);
      if (dotMatch) {
        const prefix = dotMatch[1];

        if (prefix === 'jdc') {
          for (const mod of JDC_SUB_MODULES) {
            suggestions.push({
              label: mod.label,
              kind: toKind(mod.kind),
              detail: mod.detail,
              documentation: mod.documentation,
              insertText: mod.label,
              range,
            });
          }
        }

        const apiKey = prefix;
        const methods = APIS[apiKey];
        if (methods) {
          for (const m of methods) {
            suggestions.push({
              label: m.label,
              kind: toKind(m.kind),
              detail: m.detail,
              documentation: m.documentation,
              insertText: m.insertText,
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
              range,
            });
          }
        }
      } else {
        for (const mod of TOP_LEVEL_MODULES) {
          suggestions.push({
            label: mod.label,
            kind: toKind(mod.kind),
            detail: mod.detail,
            documentation: mod.documentation,
            insertText: mod.label,
            range,
          });
        }
        for (const cv of CONTEXT_VARIABLES) {
          suggestions.push({
            label: cv.label,
            kind: toKind(cv.kind),
            detail: cv.detail,
            documentation: cv.documentation,
            insertText: cv.label,
            range,
          });
        }
      }

      return { suggestions };
    },
  });
}
