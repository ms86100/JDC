package com.jira.issue.controller;

import com.jira.issue.entity.CustomFieldDefinition;
import com.jira.issue.entity.CustomFieldOption;
import com.jira.issue.service.CustomFieldDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/custom-fields")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CustomFieldController {

    private final CustomFieldDefinitionService definitionService;

    @GetMapping
    public ResponseEntity<List<CustomFieldDefinition>> listAll() {
        return ResponseEntity.ok(definitionService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomFieldDefinition> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(definitionService.getById(id));
    }

    @GetMapping("/key/{fieldKey}")
    public ResponseEntity<CustomFieldDefinition> getByKey(@PathVariable String fieldKey) {
        return ResponseEntity.ok(definitionService.getByKey(fieldKey));
    }

    @GetMapping("/type/{fieldType}")
    public ResponseEntity<List<CustomFieldDefinition>> listByType(@PathVariable String fieldType) {
        return ResponseEntity.ok(definitionService.listByType(fieldType));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomFieldDefinition>> search(@RequestParam String name) {
        return ResponseEntity.ok(definitionService.search(name));
    }

    @PostMapping
    public ResponseEntity<CustomFieldDefinition> create(@RequestBody CustomFieldDefinition definition) {
        return ResponseEntity.status(HttpStatus.CREATED).body(definitionService.create(definition));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomFieldDefinition> update(@PathVariable UUID id,
                                                        @RequestBody CustomFieldDefinition update) {
        return ResponseEntity.ok(definitionService.update(id, update));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        definitionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getRegisteredFieldTypes() {
        return ResponseEntity.ok(definitionService.getRegisteredFieldTypes());
    }

    // === Option Management ===

    @GetMapping("/{fieldId}/options")
    public ResponseEntity<List<CustomFieldOption>> getOptions(@PathVariable UUID fieldId) {
        return ResponseEntity.ok(definitionService.getOptions(fieldId));
    }

    @GetMapping("/{fieldId}/options/enabled")
    public ResponseEntity<List<CustomFieldOption>> getEnabledOptions(@PathVariable UUID fieldId) {
        return ResponseEntity.ok(definitionService.getEnabledOptions(fieldId));
    }

    @GetMapping("/{fieldId}/options/parents")
    public ResponseEntity<List<CustomFieldOption>> getParentOptions(@PathVariable UUID fieldId) {
        return ResponseEntity.ok(definitionService.getParentOptions(fieldId));
    }

    @GetMapping("/options/{parentOptionId}/children")
    public ResponseEntity<List<CustomFieldOption>> getChildOptions(@PathVariable UUID parentOptionId) {
        return ResponseEntity.ok(definitionService.getChildOptions(parentOptionId));
    }

    @PostMapping("/{fieldId}/options")
    public ResponseEntity<CustomFieldOption> addOption(@PathVariable UUID fieldId,
                                                       @RequestBody CustomFieldOption option) {
        return ResponseEntity.status(HttpStatus.CREATED).body(definitionService.addOption(fieldId, option));
    }

    @PutMapping("/options/{optionId}")
    public ResponseEntity<CustomFieldOption> updateOption(@PathVariable UUID optionId,
                                                          @RequestBody CustomFieldOption update) {
        return ResponseEntity.ok(definitionService.updateOption(optionId, update));
    }

    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable UUID optionId) {
        definitionService.deleteOption(optionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{fieldId}/options/reorder")
    public ResponseEntity<Void> reorderOptions(@PathVariable UUID fieldId,
                                                @RequestBody List<UUID> orderedOptionIds) {
        definitionService.reorderOptions(fieldId, orderedOptionIds);
        return ResponseEntity.ok().build();
    }
}
