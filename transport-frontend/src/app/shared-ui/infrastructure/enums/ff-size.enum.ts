export type FfSize = 'sm' | 'md' | 'lg';

export type FfButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';

export enum FfPermissionAction {
  VIEW = 'view',
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  APPROVE = 'approve',
  PRINT = 'print',
  EXPORT = 'export',
  IMPORT = 'import'
}
