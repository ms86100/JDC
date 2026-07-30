package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.entity.WorkflowTransition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces transition-level metadata: {@code permission_check} and {@code user_group_ids} on {@link WorkflowTransition}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransitionPermissionEvaluator {

    private final ProjectPermissionClient projectPermissionClient;

    @Value("${app.workflow.permission.default-suffix:_ISSUES}")
    private String permissionDefaultSuffix;

    @Value("${app.workflow.permission.bypass-edit:EDIT_ISSUES}")
    private String bypassEditPermission;

    @Value("${app.workflow.permission.bypass-resolve:RESOLVE_ISSUES}")
    private String bypassResolvePermission;

    public boolean canPerformTransition(WorkflowTransition transition, WorkflowContext ctx) {
        if (ctx.getUserId() == null || ctx.getProjectId() == null) {
            return true;
        }

        if (hasGrantedPermission(ctx, bypassEditPermission) || hasGrantedPermission(ctx, bypassResolvePermission)) {
            return true;
        }

        String permission = transition.getPermissionCheck();
        if (permission != null && !permission.isBlank()) {
            String normalized = normalizePermissionKey(permission);
            if (!projectPermissionClient.hasPermission(ctx.getUserId(), ctx.getProjectId(), normalized)) {
                log.debug("Transition {} blocked: missing permission {}", transition.getName(), normalized);
                return false;
            }
        }

        List<String> groupIds = transition.getUserGroupIds();
        if (groupIds != null && !groupIds.isEmpty()) {
            if (!userInAllowedGroups(ctx.getUserData(), groupIds)) {
                log.debug("Transition {} blocked: user not in required groups", transition.getName());
                return false;
            }
        }

        return true;
    }

    private boolean hasGrantedPermission(WorkflowContext ctx, String permission) {
        if (ctx.getUserData() == null || permission == null) {
            return false;
        }
        Object raw = ctx.getUserData().get("permissions");
        if (raw instanceof List<?> list) {
            return list.stream().anyMatch(permission::equals);
        }
        return false;
    }

    public String requiredPermissionLabel(WorkflowTransition transition) {
        if (transition.getPermissionCheck() != null && !transition.getPermissionCheck().isBlank()) {
            return normalizePermissionKey(transition.getPermissionCheck());
        }
        return null;
    }

    private String normalizePermissionKey(String raw) {
        String key = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (key.contains("_")) {
            return key;
        }
        return key + permissionDefaultSuffix;
    }

    @SuppressWarnings("unchecked")
    private boolean userInAllowedGroups(Map<String, Object> userData, List<String> requiredGroupIds) {
        Object groups = userData.get("groups");
        if (!(groups instanceof List<?> userGroups)) {
            return false;
        }
        for (String required : requiredGroupIds) {
            for (Object g : userGroups) {
                String gid = String.valueOf(g);
                if (required.equalsIgnoreCase(gid) || required.equalsIgnoreCase(String.valueOf(g))) {
                    return true;
                }
            }
        }
        return false;
    }
}
