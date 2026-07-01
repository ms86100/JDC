import type { ProjectResponse } from '../api/projectApi';

/** Map API project fields to sidebar/overview template key (SCRUM, KANBAN, …). */
export function resolveProjectTemplate(project?: ProjectResponse | null): string | undefined {
  if (!project) return undefined;
  if (project.template) return project.template;
  const cat = (project.category ?? '').toLowerCase();
  if (cat === 'scrum') return 'SCRUM';
  if (cat === 'kanban') return 'KANBAN';
  if (cat === 'task' || cat === 'task_management') return 'TASK_MANAGEMENT';
  if (cat === 'process' || cat === 'process_management') return 'PROCESS_MANAGEMENT';
  if (cat === 'project' || cat === 'project_management') return 'PROJECT_MANAGEMENT';
  return undefined;
}
