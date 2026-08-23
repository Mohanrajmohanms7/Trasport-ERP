export function ffTrackById<T extends { id?: string | number }>(
  _index: number,
  item: T
): string | number {
  return item?.id ?? _index;
}

export function ffTrackByField<T>(field: keyof T) {
  return (_index: number, item: T): unknown => item?.[field] ?? _index;
}

export function ffResolveErrorMessage(
  errors: Record<string, unknown> | null,
  customMessages?: Record<string, string>,
  defaults?: Record<string, string>
): string {
  if (!errors) return '';
  const key = Object.keys(errors)[0];
  if (!key) return '';
  if (customMessages?.[key]) return customMessages[key];
  if (defaults?.[key]) return defaults[key];
  return `Invalid: ${key}`;
}
