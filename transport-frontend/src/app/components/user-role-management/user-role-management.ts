import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { HttpClient } from '@angular/common/http';
import { FormValidationDirective } from '../../shared/form-validation.directive';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserRoleService, User, Role, Permission } from '../../services/user-role.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import {
  FfDropdownComponent,
  FfSelectOption,
  FfTextboxComponent,
  FfPasswordComponent,
  FfTextareaComponent,
  FfCheckboxComponent,
  FfButtonComponent
} from '@ff/ui';

@Component({
  selector: 'app-user-role-management',
  standalone: true,
  imports: [FormValidationDirective, 
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfPasswordComponent,
    FfTextareaComponent,
    FfCheckboxComponent,
    FfButtonComponent
  ],
  templateUrl: './user-role-management.html',
  styles: []
})
export class UserRoleManagementComponent implements OnInit {
  // ---- Permissions tab (role x module x View / Add / Edit / Delete / Approve) — enforced by the server
  private permHttp = inject(HttpClient);
  readonly permActions = ['view', 'add', 'edit', 'delete', 'approve'];
  permRoles = signal<string[]>([]);
  permRole = signal('');
  permGrid = signal<any>(null);
  permBusy = signal(false);
  loadPermRoles(): void {
    this.permHttp.get<any>('/api/v1/role-permissions/roles').subscribe({ next: r => this.permRoles.set(r?.data || []) });
  }
  loadPermGrid(role: string): void {
    this.permRole.set(role); this.permGrid.set(null);
    if (!role) return;
    this.permHttp.get<any>('/api/v1/role-permissions', { params: { role } }).subscribe({ next: r => this.permGrid.set(r?.data) });
  }
  togglePerm(m: any, a: string, on: boolean): void {
    m[a] = on;
    if (a === 'view' && !on) for (const x of this.permActions) m[x] = false;   // no View = nothing else
    this.permGrid.set({ ...this.permGrid() });
  }
  savePerms(): void {
    const g = this.permGrid(); if (!g || this.permBusy()) return;
    this.permBusy.set(true);
    const rows = g.modules.map((m: any) => ({ module: m.module, view: m.view, add: m.add, edit: m.edit, delete: m.delete, approve: m.approve }));
    this.permHttp.put<any>(`/api/v1/role-permissions/${encodeURIComponent(this.permRole())}`, rows).subscribe({
      next: r => { this.permBusy.set(false); this.permGrid.set(r?.data); this.notifyPerm('Permissions saved for ' + this.permRole(), true); },
      error: e => { this.permBusy.set(false); this.notifyPerm(e?.error?.errors?.[0] || e?.error?.message || 'Could not save', false); }
    });
  }
  resetPerms(): void {
    if (this.permBusy()) return;
    this.permBusy.set(true);
    this.permHttp.delete<any>(`/api/v1/role-permissions/${encodeURIComponent(this.permRole())}`).subscribe({
      next: r => { this.permBusy.set(false); this.permGrid.set(r?.data); this.notifyPerm(this.permRole() + ' has full access again', true); },
      error: e => { this.permBusy.set(false); this.notifyPerm(e?.error?.errors?.[0] || e?.error?.message || 'Could not reset', false); }
    });
  }
  private permToast = inject(FfNotificationService);
  private notifyPerm(msg: string, ok: boolean): void { if (ok) this.permToast.success(msg); else this.permToast.error(msg); }

  private userRoleService = inject(UserRoleService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);

  // Lists
  users = signal<User[]>([]);
  roles = signal<Role[]>([]);
  permissions = signal<Permission[]>([]);

  statusOptions: FfSelectOption[] = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Inactive', value: 'INACTIVE' }
  ];

  roleOptions = computed<FfSelectOption[]>(() => {
    return this.roles()
      .filter(r => r.status === 'ACTIVE')
      .map(r => ({
        label: `${r.name} (${r.code})`,
        value: r.id
      }));
  });

  // States
  activeTab = signal<string>('users'); // 'users' | 'roles'
  loading = signal<boolean>(false);
  showUserEditor = signal<boolean>(false);
  showRoleEditor = signal<boolean>(false);
  editingUser = signal<User | null>(null);
  editingRole = signal<Role | null>(null);

  // Forms
  userForm!: FormGroup;
  roleForm!: FormGroup;

  ngOnInit() {
    this.initForms();
    this.loadUsers();
    this.loadRoles();
    this.loadPermissions();
  }

  initForms() {
    this.userForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      username: ['', [Validators.required, Validators.maxLength(50)]],
      password: [''], // Required only for new users
      email: ['', [Validators.email]],
      phone: ['', [Validators.maxLength(20)]],
      roleId: ['', Validators.required],
      status: ['ACTIVE', Validators.required],
      description: ['']
    });

    this.roleForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: [''],
      status: ['ACTIVE', Validators.required]
    });
  }

  loadUsers() {
    this.loading.set(true);
    this.userRoleService.getUsers().subscribe(res => {
      if (res.success && res.data) {
        this.users.set(res.data.content || res.data);
      }
      this.loading.set(false);
    }, () => this.loading.set(false));
  }

  loadRoles() {
    this.userRoleService.getRoles().subscribe(res => {
      if (res.success && res.data) {
        this.roles.set(res.data);
      }
    });
  }

  loadPermissions() {
    this.userRoleService.getPermissions().subscribe(res => {
      if (res.success && res.data) {
        this.permissions.set(res.data);
      }
    });
  }

  openAddUser() {
    this.editingUser.set(null);
    this.userForm.reset({ status: 'ACTIVE', roleId: '' });
    this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserEditor.set(true);
  }

  openEditUser(user: User) {
    this.editingUser.set(user);
    const firstRoleId = user.roles && user.roles.length > 0 ? user.roles[0].id : '';
    this.userForm.patchValue({
      ...user,
      roleId: firstRoleId
    });
    this.userForm.get('password')?.clearValidators();
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserEditor.set(true);
  }

  saveUser() {
    if (this.userForm.invalid) return;
    const val = this.userForm.value;
    const userObj = this.editingUser();
    const selectedRoleId = val.roleId;
    const userRoles = selectedRoleId ? [{ id: Number(selectedRoleId) }] : [];

    const payload = {
      ...val,
      roles: userRoles
    };

    if (userObj && userObj.id) {
      this.userRoleService.updateUser(userObj.id, payload).subscribe(() => {
        this.loadUsers();
        this.showUserEditor.set(false);
      });
    } else {
      this.userRoleService.createUser(payload).subscribe(() => {
        this.loadUsers();
        this.showUserEditor.set(false);
      });
    }
  }

  deleteUser(user: User) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete User Account',
        message: `Are you sure you want to delete user: ${user.name}? This will revoke their platform credentials.`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && user.id) {
        this.userRoleService.deleteUser(user.id).subscribe(() => this.loadUsers());
      }
    });
  }

  openAddRole() {
    this.editingRole.set(null);
    this.roleForm.reset({ status: 'ACTIVE' });
    this.showRoleEditor.set(true);
  }

  openEditRole(role: Role) {
    this.editingRole.set(role);
    this.roleForm.patchValue(role);
    this.showRoleEditor.set(true);
  }

  saveRole() {
    if (this.roleForm.invalid) return;
    const val = this.roleForm.value;
    const roleObj = this.editingRole();

    if (roleObj && roleObj.id) {
      this.userRoleService.updateRole(roleObj.id, val).subscribe(() => {
        this.loadRoles();
        this.showRoleEditor.set(false);
      });
    } else {
      this.userRoleService.createRole(val).subscribe(() => {
        this.loadRoles();
        this.showRoleEditor.set(false);
      });
    }
  }

  deleteRole(role: Role) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Role',
        message: `Are you sure you want to delete role: ${role.name}? Users assigned this role will lose permissions.`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && role.id) {
        this.userRoleService.deleteRole(role.id).subscribe(() => this.loadRoles());
      }
    });
  }

  // Toggles role permission linkage
  toggleRolePermission(role: Role, permission: Permission) {
    if (!role.id || !permission.id) return;
    
    let updatedPermissions = [...(role.permissions || [])];
    const index = updatedPermissions.findIndex(p => p.id === permission.id);
    
    if (index >= 0) {
      updatedPermissions.splice(index, 1);
    } else {
      updatedPermissions.push(permission);
    }

    const payload = { ...role, permissions: updatedPermissions };
    this.userRoleService.updateRole(role.id, payload).subscribe(() => {
      this.loadRoles();
    });
  }

  hasPermission(role: Role, permCode: string): boolean {
    return !!role.permissions?.some(p => p.code === permCode);
  }
}
