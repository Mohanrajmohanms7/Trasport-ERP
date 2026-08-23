/** Material Symbols icon names used across FleetFlow UI */
export const FfIconRegistry = {
  search: 'search',
  add: 'add',
  edit: 'edit',
  delete: 'delete',
  visibility: 'visibility',
  visibilityOff: 'visibility_off',
  close: 'close',
  check: 'check',
  warning: 'warning',
  error: 'error',
  info: 'info',
  filter: 'filter_list',
  download: 'download',
  upload: 'upload',
  print: 'print',
  moreVert: 'more_vert',
  chevronLeft: 'chevron_left',
  chevronRight: 'chevron_right',
  expandMore: 'expand_more',
  person: 'person',
  lock: 'lock',
  mail: 'mail',
  calendar: 'calendar_today',
  localShipping: 'local_shipping',
  dashboard: 'dashboard'
} as const;

export type FfIconName = keyof typeof FfIconRegistry;
