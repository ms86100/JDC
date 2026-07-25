package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class MutationBuffer {

    private final List<Mutation> mutations = new CopyOnWriteArrayList<>();
    private boolean committed = false;

    public void addMutation(String type, String target, Map<String, Object> data) {
        if (committed) {
            log.warn("Attempt to buffer mutation after commit — executing immediately");
            return;
        }
        mutations.add(new Mutation(type, target, data));
    }

    public List<Mutation> getMutations() {
        return Collections.unmodifiableList(mutations);
    }

    public int size() {
        return mutations.size();
    }

    public void markCommitted() {
        this.committed = true;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void clear() {
        mutations.clear();
    }

    public record Mutation(String type, String target, Map<String, Object> data) {
        public static final String SET_FIELD = "SET_FIELD";
        public static final String ADD_COMMENT = "ADD_COMMENT";
        public static final String ADD_LABEL = "ADD_LABEL";
        public static final String REMOVE_LABEL = "REMOVE_LABEL";
        public static final String ADD_WATCHER = "ADD_WATCHER";
        public static final String REMOVE_WATCHER = "REMOVE_WATCHER";
    }
}
