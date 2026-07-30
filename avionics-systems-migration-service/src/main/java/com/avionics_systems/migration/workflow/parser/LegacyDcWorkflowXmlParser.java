package com.avionics_systems.migration.workflow.parser;

import com.avionics_systems.migration.exception.ValidationException;
import com.avionics_systems.migration.workflow.model.*;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;

/**
 * Secure StAX parser for Legacy DC OSWorkflow {@code workflow-descriptor} exports.
 */
@Component
public class LegacyDcWorkflowXmlParser {

    private static final int MAX_ELEMENTS = 50_000;
    private static final int MAX_DEPTH = 64;

    public WorkflowDescriptorModel parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new ValidationException("Workflow XML is empty", "WORKFLOW_XML_EMPTY", "file");
        }
        String trimmed = xml.trim();
        if (!trimmed.contains("workflow-descriptor")) {
            throw new ValidationException("Not a workflow-descriptor document", "WORKFLOW_XML_FORMAT", "file");
        }
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(trimmed));

            WorkflowDescriptorModel.WorkflowDescriptorModelBuilder root = WorkflowDescriptorModel.builder();
            List<WorkflowActionModel> initialActions = new ArrayList<>();
            List<WorkflowActionModel> commonActions = new ArrayList<>();
            List<WorkflowStepModel> steps = new ArrayList<>();
            Map<String, String> rootMeta = new LinkedHashMap<>();

            int elementCount = 0;
            int depth = 0;
            Deque<String> stack = new ArrayDeque<>();
            WorkflowStepModel currentStep = null;
            WorkflowActionModel currentAction = null;
            String collectingMetaFor = null;
            String pendingMetaName = null;
            String pendingViewName = null;
            boolean inValidators = false;
            boolean inPostFunctions = false;
            boolean inRestrictTo = false;
            boolean inResults = false;
            WorkflowResultModel currentResult = null;
            WorkflowFunctionDescriptor currentFunction = null;
            String pendingArgName = null;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    elementCount++;
                    if (elementCount > MAX_ELEMENTS) {
                        throw new ValidationException("Workflow XML exceeds element limit", "WORKFLOW_XML_TOO_LARGE", "file");
                    }
                    depth = stack.size();
                    if (depth > MAX_DEPTH) {
                        throw new ValidationException("Workflow XML exceeds nesting depth", "WORKFLOW_XML_DEPTH", "file");
                    }
                    String local = reader.getLocalName();
                    stack.push(local);

                    switch (local) {
                        case "workflow-descriptor" -> root.name(attr(reader, "name"));
                        case "meta" -> {
                            pendingMetaName = attr(reader, "name");
                            collectingMetaFor = stack.size() > 2 ? stack.toArray(new String[0])[1] : "workflow-descriptor";
                        }
                        case "initial-actions" -> { /* container */ }
                        case "common-actions" -> { /* container */ }
                        case "steps" -> { /* container */ }
                        case "step" -> {
                            currentStep = WorkflowStepModel.builder()
                                    .id(attr(reader, "id"))
                                    .name(attr(reader, "name"))
                                    .build();
                        }
                        case "action" -> {
                            currentAction = WorkflowActionModel.builder()
                                    .id(attr(reader, "id"))
                                    .name(attr(reader, "name"))
                                    .sourceStepId(currentStep != null ? currentStep.getId() : null)
                                    .global(isUnder(stack, "common-actions"))
                                    .initial(isUnder(stack, "initial-actions"))
                                    .build();
                        }
                        case "view" -> pendingViewName = attr(reader, "name");
                        case "validators" -> inValidators = true;
                        case "post-functions" -> inPostFunctions = true;
                        case "restrict-to" -> inRestrictTo = true;
                        case "results" -> inResults = true;
                        case "unconditional-result" -> {
                            currentResult = WorkflowResultModel.builder()
                                    .type("unconditional-result")
                                    .oldStatus(attr(reader, "old-status"))
                                    .status(attr(reader, "status"))
                                    .targetStepId(attr(reader, "step"))
                                    .build();
                        }
                        case "result" -> {
                            currentResult = WorkflowResultModel.builder()
                                    .type("result")
                                    .oldStatus(attr(reader, "old-status"))
                                    .status(attr(reader, "status"))
                                    .targetStepId(attr(reader, "step"))
                                    .build();
                        }
                        case "conditions" -> {
                            if (currentFunction == null && (inRestrictTo || inResults)) {
                                currentFunction = WorkflowFunctionDescriptor.builder()
                                        .conditionLogicType(attr(reader, "type"))
                                        .build();
                            }
                        }
                        case "condition", "validator", "function" -> {
                            currentFunction = WorkflowFunctionDescriptor.builder()
                                    .type(attr(reader, "type"))
                                    .build();
                        }
                        case "arg" -> pendingArgName = attr(reader, "name");
                        default -> { /* ignore */ }
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    String text = reader.getText().trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    if (pendingMetaName != null) {
                        if ("workflow-descriptor".equals(collectingMetaFor) || stack.peek() != null && stack.contains("workflow-descriptor")) {
                            if (currentStep == null && currentAction == null) {
                                rootMeta.put(pendingMetaName, text);
                            } else if (currentAction != null && currentStep == null) {
                                currentAction.getMeta().put(pendingMetaName, text);
                            } else if (currentStep != null && currentAction == null) {
                                currentStep.getMeta().put(pendingMetaName, text);
                            } else if (currentAction != null) {
                                currentAction.getMeta().put(pendingMetaName, text);
                            }
                        }
                        pendingMetaName = null;
                    }
                    if (pendingArgName != null && currentFunction != null) {
                        if ("class.name".equals(pendingArgName)) {
                            currentFunction.setClassName(text);
                        } else {
                            currentFunction.getArgs().put(pendingArgName, text);
                        }
                        pendingArgName = null;
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = reader.getLocalName();
                    switch (local) {
                        case "meta" -> collectingMetaFor = null;
                        case "view" -> {
                            if (currentAction != null && pendingViewName != null) {
                                currentAction.setView(pendingViewName);
                            }
                            pendingViewName = null;
                        }
                        case "unconditional-result", "result" -> {
                            if (currentAction != null && currentResult != null) {
                                currentAction.getResults().add(currentResult);
                            }
                            currentResult = null;
                        }
                        case "results" -> inResults = false;
                        case "restrict-to" -> inRestrictTo = false;
                        case "validators" -> inValidators = false;
                        case "post-functions" -> inPostFunctions = false;
                        case "validator", "function", "condition" -> attachFunction(currentAction, currentResult, inValidators, inPostFunctions, inRestrictTo, currentFunction);
                        case "conditions" -> {
                            if (currentAction != null && inRestrictTo && currentFunction != null) {
                                currentAction.getConditions().add(currentFunction);
                            } else if (currentResult != null && currentFunction != null) {
                                currentResult.getConditions().add(currentFunction);
                            }
                            currentFunction = null;
                        }
                        case "action" -> {
                            if (currentAction != null) {
                                if (currentAction.isInitial()) {
                                    initialActions.add(currentAction);
                                } else if (currentAction.isGlobal()) {
                                    commonActions.add(currentAction);
                                } else if (currentStep != null) {
                                    currentStep.getActions().add(currentAction);
                                }
                            }
                            currentAction = null;
                        }
                        case "step" -> {
                            if (currentStep != null) {
                                steps.add(currentStep);
                            }
                            currentStep = null;
                        }
                        case "workflow-descriptor" -> { /* done */ }
                        default -> { /* ignore */ }
                    }
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                }
            }
            reader.close();

            WorkflowDescriptorModel model = root
                    .meta(rootMeta)
                    .initialActions(initialActions)
                    .commonActions(commonActions)
                    .steps(steps)
                    .build();

            if (model.getName() == null || model.getName().isBlank()) {
                throw new ValidationException("workflow-descriptor name is required", "WORKFLOW_NAME_REQUIRED", "name");
            }
            if (model.getSteps().isEmpty()) {
                throw new ValidationException("workflow-descriptor must contain at least one step", "WORKFLOW_NO_STEPS", "steps");
            }
            return model;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to parse workflow XML: " + e.getMessage(), "WORKFLOW_XML_PARSE", "file");
        }
    }

    private static void attachFunction(WorkflowActionModel action, WorkflowResultModel result,
                                       boolean validators, boolean postFunctions, boolean restrictTo,
                                       WorkflowFunctionDescriptor fn) {
        if (fn == null) {
            return;
        }
        if (validators && action != null) {
            action.getValidators().add(fn);
        } else if (postFunctions && action != null) {
            action.getPostFunctions().add(fn);
        } else if (restrictTo && action != null) {
            action.getConditions().add(fn);
        } else if (result != null) {
            result.getConditions().add(fn);
        }
    }

    private static boolean isUnder(Deque<String> stack, String container) {
        return stack.stream().anyMatch(container::equals);
    }

    private static String attr(XMLStreamReader reader, String name) {
        String v = reader.getAttributeValue(null, name);
        return v != null ? v : reader.getAttributeValue(XMLConstants.NULL_NS_URI, name);
    }
}
