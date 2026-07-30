/**
 * Normalize list API payloads (raw array, Spring page, or axios-wrapped body).
 */
export function asArray<T>(value: unknown): T[] {
  if (value == null) return [];
  if (Array.isArray(value)) return value as T[];
  if (typeof value !== 'object') return [];

  const record = value as Record<string, unknown>;

  // Axios response shape when query cache was populated by a hook without `select`
  if ('data' in record) {
    return asArray<T>(record.data);
  }

  if (Array.isArray(record.content)) return record.content as T[];
  if (Array.isArray(record.items)) return record.items as T[];
  if (Array.isArray(record.results)) return record.results as T[];

  return [];
}
