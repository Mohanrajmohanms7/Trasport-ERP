import { ValidatorFn, AsyncValidatorFn } from '@angular/forms';
import { FfSize } from '../enums/ff-size.enum';

/** Shared config contract for all ff-* form controls */
export interface FfControlConfig {
  label?: string;
  hint?: string;
  placeholder?: string;
  id?: string;
  disabled?: boolean;
  readonly?: boolean;
  required?: boolean;
  loading?: boolean;
  skeleton?: boolean;
  validators?: ValidatorFn[];
  asyncValidators?: AsyncValidatorFn[];
  errorMessages?: Record<string, string>;
  prefixIcon?: string;
  suffixIcon?: string;
  tooltip?: string;
  permission?: string;
  ruleId?: string;
  size?: FfSize;
  labelKey?: string;
}

export interface FfSelectOption<T = unknown> {
  label: string;
  value: T;
  disabled?: boolean;
  group?: string;
}

export interface FfBreadcrumbItem {
  label: string;
  route?: string;
}

export interface FfPageAction {
  id: string;
  label: string;
  icon?: string;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  permission?: string;
  disabled?: boolean;
}

export interface FfGridColumn {
  field: string;
  header: string;
  type?: 'text' | 'number' | 'currency' | 'date' | 'badge' | 'avatar' | 'actions' | 'custom';
  width?: string;
  sortable?: boolean;
  filterable?: boolean;
  sticky?: boolean;
  align?: 'left' | 'center' | 'right';
  badgeMap?: Record<string, { color: string; label?: string }>;
}

export interface FfGridAction {
  id: string;
  icon: string;
  label: string;
  permission?: string;
  color?: string;
}

export interface FfGridConfig {
  columns: FfGridColumn[];
  totalRecords?: number;
  pageSize?: number;
  pageIndex?: number;
  sortable?: boolean;
  filterable?: boolean;
  paginated?: boolean;
  selectable?: boolean | 'multiple';
  stickyHeader?: boolean;
  rowActions?: FfGridAction[];
  emptyMessage?: string;
  trackByField?: string;
}

export interface FfSortEvent {
  field: string;
  direction: 'asc' | 'desc' | '';
}

export interface FfPageEvent {
  pageIndex: number;
  pageSize: number;
}

export interface FfGridActionEvent<T = unknown> {
  action: string;
  row: T;
}

export interface FfBusinessRuleEffect {
  visible?: boolean;
  hidden?: boolean;
  required?: boolean;
  readonly?: boolean;
  disabled?: boolean;
  defaultValue?: unknown;
  tooltip?: string;
  label?: string;
}

export interface FfBusinessRule {
  id: string;
  entity: string;
  field?: string;
  effects: FfBusinessRuleEffect;
}
