/** Human-readable label for migration custom field type keys (incl. Avionics Systems plugin ids). */
export function formatCustomFieldType(type: string): string {
  if (!type || !type.trim()) {
    return 'Text';
  }
  const t = type.trim();
  const lower = t.toLowerCase();

  const friendlyByKey: Record<string, string> = {
    text: 'Text',
    number: 'Number',
    datepicker: 'Date',
    datetime: 'Date & time',
    select: 'Select list',
    multiselect: 'Multi-select',
    checkbox: 'Checkboxes',
    textarea: 'Text area',
    url: 'URL',
    userpicker: 'User picker',
  };
  if (friendlyByKey[lower]) {
    return friendlyByKey[lower];
  }

  if (lower.includes('customfieldtypes:')) {
    const suffix = (t.split(':').pop() ?? t).toLowerCase();
    const avisysMap: Record<string, string> = {
      textfield: 'Text',
      textarea: 'Text area',
      textsearcher: 'Text',
      float: 'Number',
      select: 'Select list',
      multiselect: 'Multi-select',
      cascadingselect: 'Cascading select',
      datepicker: 'Date',
      datetime: 'Date & time',
      userpicker: 'User picker',
      multiuserpicker: 'Multi user picker',
      grouppicker: 'Group picker',
      version: 'Version',
      project: 'Project',
      labels: 'Labels',
      url: 'URL',
      readonlyfield: 'Read-only',
      importid: 'Import id',
      'gh-epic-link': 'Epic link',
      'gh-epic-label': 'Epic name',
    };
    if (avisysMap[suffix]) {
      return avisysMap[suffix];
    }
    return suffix
      .replace(/[-_]+/g, ' ')
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  return t;
}

/** Hide internal Atlassian plugin keys from the UI. */
export function isInternalFieldKey(key: string): boolean {
  const k = key.toLowerCase();
  return k.includes('atlassian') || k.includes('customfieldtypes') || k.startsWith('com.');
}

export function formatFieldKeyForDisplay(key: string): string | null {
  if (!key || isInternalFieldKey(key)) {
    return null;
  }
  return key;
}
