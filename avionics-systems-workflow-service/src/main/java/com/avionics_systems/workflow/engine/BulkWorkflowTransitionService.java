package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulkWorkflowTransitionService {

    private final WorkflowExecutionEngine executionEngine;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkTransitionExecutionResponse executeBulk(BulkExecuteTransitionRequest request) {
        List<BulkTransitionResultItem> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (BulkTransitionItem item : request.getItems()) {
            ExecuteTransitionRequest single = new ExecuteTransitionRequest();
            single.setIssueId(item.getIssueId());
            single.setProjectId(request.getProjectId());
            single.setUserId(request.getUserId());
            single.setTransitionId(item.getTransitionId());
            single.setStatusId(item.getStatusId());
            single.setComment(item.getComment());
            single.setResolutionId(item.getResolutionId());
            single.setScreenInput(item.getScreenInput());

            TransitionExecutionResponse response = executionEngine.execute(single);
            if (response.isSuccess()) {
                succeeded++;
            } else {
                failed++;
            }
            results.add(BulkTransitionResultItem.builder()
                    .issueId(item.getIssueId())
                    .success(response.isSuccess())
                    .newStatusId(response.getNewStatusId())
                    .error(response.getError())
                    .errors(response.getErrors())
                    .build());
        }

        return BulkTransitionExecutionResponse.builder()
                .total(request.getItems().size())
                .succeeded(succeeded)
                .failed(failed)
                .results(results)
                .build();
    }
}
