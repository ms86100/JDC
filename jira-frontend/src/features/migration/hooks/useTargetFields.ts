import { useCallback } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  fieldApi,
  type CustomFieldDefinitionDto,
  type FieldDefinitionDto,
  type ServerFieldMappingDto,
} from '../../../api/fieldApi';
import type { FieldMapping, MigrationTargetField } from '../types/migration';

/** Fallback when field API is empty or unreachable */
export const DEFAULT_TARGET_FIELDS: MigrationTargetField[] = [
  { field: 'issueKey', displayName: 'Issue Key', dataType: 'STRING', required: false, description: 'External issue key (e.g. PROJ-1)' },
  { field: 'summary', displayName: 'Summary', dataType: 'STRING', required: true, description: 'Issue title' },
  { field: 'description', displayName: 'Description', dataType: 'TEXT', required: false },
  { field: 'issuetype', displayName: 'Issue Type', dataType: 'ENUM', required: true },
  { field: 'priority', displayName: 'Priority', dataType: 'ENUM', required: false },
  { field: 'status', displayName: 'Status', dataType: 'ENUM', required: false },
  { field: 'project', displayName: 'Project Key', dataType: 'STRING', required: true },
  { field: 'assignee', displayName: 'Assignee', dataType: 'USER', required: false },
  { field: 'reporter', displayName: 'Reporter', dataType: 'USER', required: false },
  { field: 'labels', displayName: 'Labels', dataType: 'ARRAY', required: false },
  { field: 'parent', displayName: 'Parent Issue', dataType: 'ISSUE', required: false },
  { field: 'epic', displayName: 'Epic Link', dataType: 'ISSUE', required: false },
];

function mapFieldType(fieldType: string): string {
  const t = fieldType?.toUpperCase() ?? 'STRING';
  if (['TEXT', 'TEXTAREA', 'RICHTEXT', 'URL', 'EMAIL'].includes(t)) return t === 'TEXTAREA' || t === 'RICHTEXT' ? 'TEXT' : 'STRING';
  if (['SINGLE_SELECT', 'RADIO', 'BOOLEAN', 'STATUS', 'PRIORITY', 'ISSUE_TYPE'].includes(t)) return 'ENUM';
  if (['MULTI_SELECT', 'LABEL', 'CHECKBOX'].includes(t)) return 'ARRAY';
  if (['NUMBER', 'CURRENCY', 'DURATION'].includes(t)) return 'NUMBER';
  if (t === 'DATETIME') return 'DATETIME';
  if (t === 'DATE' || t === 'TIME') return 'DATE';
  if (['USER', 'GROUP'].includes(t)) return 'USER';
  if (['PARENT_ISSUE', 'EPIC', 'SUBTASK'].includes(t)) return 'ISSUE';
  if (t === 'VERSION') return 'VERSION';
  return 'STRING';
}

function definitionToTargetField(def: FieldDefinitionDto): MigrationTargetField | null {
  if (def.hidden || def.deprecated) return null;
  return {
    field: def.fieldKey,
    displayName: def.displayName || def.fieldKey,
    dataType: mapFieldType(def.fieldType),
    required: Boolean(def.required),
    description: def.description,
  };
}

function customToTargetField(def: CustomFieldDefinitionDto): MigrationTargetField | null {
  if (!def.enabled && def.enabled !== undefined) return null;
  return {
    field: def.fieldKey,
    displayName: def.name || def.fieldKey,
    dataType: mapFieldType(def.type),
    required: false,
    description: def.description,
  };
}

function mergeTargetFields(
  definitions: FieldDefinitionDto[],
  custom: CustomFieldDefinitionDto[]
): MigrationTargetField[] {
  const byKey = new Map<string, MigrationTargetField>();
  for (const def of definitions) {
    const mapped = definitionToTargetField(def);
    if (mapped) byKey.set(mapped.field, mapped);
  }
  for (const def of custom) {
    const mapped = customToTargetField(def);
    if (mapped && !byKey.has(mapped.field)) byKey.set(mapped.field, mapped);
  }
  return Array.from(byKey.values()).sort((a, b) => a.displayName.localeCompare(b.displayName));
}

function serverMappingsToFieldMappings(
  headers: string[],
  serverMappings: ServerFieldMappingDto[],
  targetFields: MigrationTargetField[]
): FieldMapping[] {
  const bySource = new Map(serverMappings.map((m) => [m.sourceKey.toLowerCase(), m]));
  return headers.map((header) => {
    const hit =
      bySource.get(header.toLowerCase()) ||
      serverMappings.find((m) => m.sourceKey === header);
    const targetKey = hit?.targetKey ?? '';
    const strategy = hit?.strategy ?? 'UNMAPPED';
    const mapped = Boolean(targetKey && strategy !== 'UNMAPPED');
    const targetInfo = targetFields.find((t) => t.field === targetKey);
    return {
      sourceColumn: header,
      targetField: targetKey,
      dataType: targetInfo?.dataType ?? 'STRING',
      required: targetInfo?.required ?? false,
      mapped,
      transformer: undefined,
    };
  });
}

export function useTargetFields(enabled = true) {
  const query = useQuery({
    queryKey: ['migration-target-fields'],
    queryFn: async () => {
      const [defsRes, customRes] = await Promise.all([
        fieldApi.getDefinitions().catch(() => ({ data: [] as FieldDefinitionDto[] })),
        fieldApi.getCustomFields().catch(() => ({ data: [] as CustomFieldDefinitionDto[] })),
      ]);
      const merged = mergeTargetFields(defsRes.data ?? [], customRes.data ?? []);
      return merged.length > 0 ? merged : DEFAULT_TARGET_FIELDS;
    },
    enabled,
    staleTime: 5 * 60 * 1000,
    retry: 1,
  });

  const autoMapMutation = useMutation({
    mutationFn: (sourceHeaders: string[]) => fieldApi.mapFields(sourceHeaders),
  });

  const autoMapFromHeaders = useCallback(
    async (
      headers: string[],
      targetFields: MigrationTargetField[]
    ): Promise<{ mappings: FieldMapping[]; typeWarnings: string[] }> => {
      const res = await autoMapMutation.mutateAsync(headers);
      const mappings = res.data?.mappings ?? [];
      const typeWarnings = res.data?.typeWarnings ?? [];
      if (mappings.length === 0) {
        return { mappings: [], typeWarnings };
      }
      return {
        mappings: serverMappingsToFieldMappings(headers, mappings, targetFields),
        typeWarnings,
      };
    },
    [autoMapMutation]
  );

  return {
    targetFields: query.data ?? DEFAULT_TARGET_FIELDS,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    autoMapFromHeaders,
    isAutoMapping: autoMapMutation.isPending,
  };
}
