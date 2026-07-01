package com.jira.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Public API for migration job rollback (wraps {@link TransactionManager}).
 */
@Service
@Slf4j
public class MigrationRollbackService {

    private final TransactionManager transactionManager;

    public MigrationRollbackService(
            @Qualifier("migrationTransactionManager") TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public TransactionManager.RollbackResult rollbackJob(UUID jobId) {
        log.info("Rollback requested for job {}", jobId);
        return transactionManager.rollbackJob(jobId);
    }

    public TransactionManager.RollbackInfo getRollbackInfo(UUID jobId) {
        return transactionManager.getRollbackInfo(jobId);
    }

    public boolean canRollback(UUID jobId) {
        return transactionManager.canRollback(jobId);
    }
}
