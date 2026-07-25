package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JdcDslTranspiler {

    // Field name mappings: SIL-style field names -> JDC API calls
    private static final Map<String, String> FIELD_GETTERS = new LinkedHashMap<>();
    private static final Map<String, String> FIELD_SETTERS = new LinkedHashMap<>();

    static {
        // Standard Jira fields
        String[] fields = {
            "summary", "description", "assignee", "reporter", "priority",
            "status", "resolution", "issueType", "environment", "dueDate",
            "labels", "components", "fixVersions", "affectsVersions",
            "storyPoints", "epicLink", "parentIssue", "securityLevel",
            "originalEstimate", "remainingEstimate", "timeSpent"
        };
        for (String f : fields) {
            FIELD_GETTERS.put(f, "jdc.issue.getFieldValue(\"" + f + "\")");
            FIELD_SETTERS.put(f, "jdc.issue.setFieldValue(\"" + f + "\", __VAL__)");
        }

        // ID-based field aliases (SIL uses plain names, JDC uses xxxId)
        FIELD_GETTERS.put("assignee", "jdc.issue.getFieldValue(\"assigneeId\")");
        FIELD_SETTERS.put("assignee", "jdc.issue.setFieldValue(\"assigneeId\", __VAL__)");
        FIELD_GETTERS.put("reporter", "jdc.issue.getFieldValue(\"reporterId\")");
        FIELD_SETTERS.put("reporter", "jdc.issue.setFieldValue(\"reporterId\", __VAL__)");
        FIELD_GETTERS.put("priority", "jdc.issue.getFieldValue(\"priorityId\")");
        FIELD_SETTERS.put("priority", "jdc.issue.setFieldValue(\"priorityId\", __VAL__)");
        FIELD_GETTERS.put("issueType", "jdc.issue.getFieldValue(\"issueTypeId\")");
        FIELD_SETTERS.put("issueType", "jdc.issue.setFieldValue(\"issueTypeId\", __VAL__)");
        FIELD_GETTERS.put("status", "jdc.issue.getFieldValue(\"statusId\")");
        FIELD_SETTERS.put("status", "jdc.issue.setFieldValue(\"statusId\", __VAL__)");
        FIELD_GETTERS.put("resolution", "jdc.issue.getFieldValue(\"resolutionId\")");
        FIELD_SETTERS.put("resolution", "jdc.issue.setFieldValue(\"resolutionId\", __VAL__)");
    }

    // SIL-style function aliases -> JDC API calls
    private static final Map<String, String> FUNCTION_ALIASES = new LinkedHashMap<>();
    static {
        // Issue functions
        FUNCTION_ALIASES.put("createIssue", "jdc.issue.createIssue");
        FUNCTION_ALIASES.put("getIssue", "jdc.issue.getIssue");
        FUNCTION_ALIASES.put("cloneIssue", "jdc.issue.cloneIssue");
        FUNCTION_ALIASES.put("deleteIssue", "jdc.issue.deleteIssue");
        FUNCTION_ALIASES.put("moveIssue", "jdc.issue.moveIssue");
        FUNCTION_ALIASES.put("linkIssue", "jdc.issue.link");
        FUNCTION_ALIASES.put("unlinkIssue", "jdc.issue.unlinkIssue");
        FUNCTION_ALIASES.put("transitionIssue", "jdc.issue.transitionIssue");
        FUNCTION_ALIASES.put("addComment", "jdc.issue.addComment");
        FUNCTION_ALIASES.put("getComments", "jdc.issue.getComments");
        FUNCTION_ALIASES.put("deleteComment", "jdc.issue.deleteComment");
        FUNCTION_ALIASES.put("getLastComment", "jdc.issue.getLastComment");
        FUNCTION_ALIASES.put("getSubTasks", "jdc.issue.getSubtasks");
        FUNCTION_ALIASES.put("getWatchers", "jdc.issue.getWatchers");
        FUNCTION_ALIASES.put("addWatcher", "jdc.issue.addWatcher");
        FUNCTION_ALIASES.put("removeWatcher", "jdc.issue.removeWatcher");
        FUNCTION_ALIASES.put("addLabel", "jdc.issue.addLabel");
        FUNCTION_ALIASES.put("removeLabel", "jdc.issue.removeLabel");
        FUNCTION_ALIASES.put("getLabels", "jdc.issue.getLabels");
        FUNCTION_ALIASES.put("addWorklog", "jdc.issue.addWorklog");
        FUNCTION_ALIASES.put("getWorklogs", "jdc.issue.getWorklogs");
        FUNCTION_ALIASES.put("getAttachments", "jdc.issue.getAttachments");
        FUNCTION_ALIASES.put("addAttachment", "jdc.issue.addAttachment");
        FUNCTION_ALIASES.put("getLinkedIssues", "jdc.issue.getLinkedIssues");
        FUNCTION_ALIASES.put("getHistory", "jdc.issue.getHistory");

        // Project functions
        FUNCTION_ALIASES.put("getProject", "jdc.project.getProject");
        FUNCTION_ALIASES.put("getProjectByKey", "jdc.project.getProjectByKey");
        FUNCTION_ALIASES.put("getVersions", "jdc.project.getVersions");
        FUNCTION_ALIASES.put("createVersion", "jdc.project.createVersion");
        FUNCTION_ALIASES.put("releaseVersion", "jdc.project.releaseVersion");
        FUNCTION_ALIASES.put("getComponents", "jdc.project.getComponents");
        FUNCTION_ALIASES.put("createComponent", "jdc.project.createComponent");

        // User functions
        FUNCTION_ALIASES.put("getUser", "jdc.user.getUser");
        FUNCTION_ALIASES.put("getCurrentUser", "jdc.user.getCurrentUser");
        FUNCTION_ALIASES.put("isUserInGroup", "jdc.user.isInGroup");
        FUNCTION_ALIASES.put("hasPermission", "jdc.user.hasPermission");

        // Search functions
        FUNCTION_ALIASES.put("jqlSearch", "jdc.search.jql");
        FUNCTION_ALIASES.put("findIssues", "jdc.search.findIssues");

        // Workflow functions
        FUNCTION_ALIASES.put("getAvailableActions", "jdc.workflow.getAvailableActions");
        FUNCTION_ALIASES.put("getAllStatuses", "jdc.workflow.getAllStatuses");

        // Email
        FUNCTION_ALIASES.put("sendEmail", "email.sendEmail");
        FUNCTION_ALIASES.put("sendToUser", "email.sendToUser");

        // HTTP
        FUNCTION_ALIASES.put("httpGet", "http.get");
        FUNCTION_ALIASES.put("httpPost", "http.post");
        FUNCTION_ALIASES.put("httpPut", "http.put");
        FUNCTION_ALIASES.put("httpDelete", "http.delete");
        FUNCTION_ALIASES.put("httpPatch", "http.patch");

        // SQL
        FUNCTION_ALIASES.put("sqlQuery", "sql.query");
        FUNCTION_ALIASES.put("sqlUpdate", "sql.update");

        // Persistent vars
        FUNCTION_ALIASES.put("setPersistentVar", "vars.set");
        FUNCTION_ALIASES.put("getPersistentVar", "vars.get");
        FUNCTION_ALIASES.put("deletePersistentVar", "vars.remove");

        // Logging
        FUNCTION_ALIASES.put("logInfo", "jdc.log.info");
        FUNCTION_ALIASES.put("logWarn", "jdc.log.warn");
        FUNCTION_ALIASES.put("logError", "jdc.log.error");
        FUNCTION_ALIASES.put("logDebug", "jdc.log.debug");

        // Sprint
        FUNCTION_ALIASES.put("getSprint", "sprint.getSprint");
        FUNCTION_ALIASES.put("moveToSprint", "sprint.moveToSprint");
        FUNCTION_ALIASES.put("moveToBacklog", "sprint.moveToBacklog");
    }

    /**
     * Transpile JDC DSL syntax to valid JavaScript.
     * Only runs if the script contains DSL markers or SIL-like patterns.
     */
    public static String transpile(String scriptBody) {
        if (scriptBody == null || scriptBody.isBlank()) return scriptBody;

        // Quick check: if script starts with "use strict" or contains module syntax, skip
        String trimmed = scriptBody.trim();
        if (trimmed.startsWith("\"use strict\"") || trimmed.startsWith("'use strict'")) {
            return scriptBody;
        }

        String result = scriptBody;

        // 1. Transpile SIL-style field ASSIGNMENTS: `fieldName = value;`
        //    Only match standalone assignments, not inside strings or already prefixed with `jdc.`
        for (Map.Entry<String, String> entry : FIELD_SETTERS.entrySet()) {
            String field = entry.getKey();
            String setter = entry.getValue();
            // Match: beginning of line (after whitespace) `fieldName = expr;` but NOT `var fieldName`, `let fieldName`, `const fieldName`, `something.fieldName`
            Pattern p = Pattern.compile(
                "(?m)^(\\s*)(?!(?:var|let|const|function|if|else|for|while|return|//|\\*)\\s)" +
                Pattern.quote(field) + "\\s*=\\s*(.+?)\\s*;",
                Pattern.MULTILINE);
            Matcher m = p.matcher(result);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String indent = m.group(1);
                String value = m.group(2);
                String replacement = indent + setter.replace("__VAL__", value) + ";";
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            if (sb.length() > 0) result = sb.toString();
        }

        // 2. Transpile SIL-style function calls: `functionName(args)` -> `jdc.xxx.functionName(args)`
        //    Only match if not already prefixed with a dot or object reference
        for (Map.Entry<String, String> entry : FUNCTION_ALIASES.entrySet()) {
            String silName = entry.getKey();
            String jdcName = entry.getValue();
            // Match standalone function call (not preceded by `.` or alphanumeric)
            result = result.replaceAll(
                "(?<![.\\w])" + Pattern.quote(silName) + "\\s*\\(",
                jdcName + "(");
        }

        // 3. Transpile SIL-style field READS in expressions (right-hand side)
        //    This is trickier — only transform when used as a standalone identifier in expressions
        //    Skip this for now — field reads via context variables (issueId, projectId etc.) already work
        //    and direct field access requires the Issue Context which we handle via the bindings

        return result;
    }

    /**
     * Check if a script body contains any DSL syntax that needs transpilation.
     */
    public static boolean containsDslSyntax(String scriptBody) {
        if (scriptBody == null) return false;
        for (String fn : FUNCTION_ALIASES.keySet()) {
            if (scriptBody.contains(fn + "(")) return true;
        }
        for (String field : FIELD_SETTERS.keySet()) {
            if (Pattern.compile("(?m)^\\s*" + Pattern.quote(field) + "\\s*=").matcher(scriptBody).find()) {
                return true;
            }
        }
        return false;
    }
}
