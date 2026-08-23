import { Component, OnInit, inject, signal } from '@angular/core';
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
  imports: [
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
    });
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
    this.userForm.reset({ status: 'ACTIVE' });
    this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserEditor.set(true);
  }

  openEditUser(user: User) {
    this.editingUser.set(user);
    this.userForm.patchValue(user);
    this.userForm.get('password')?.clearValidators();
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserEditor.set(true);
  }

  saveUser() {
    if (this.userForm.invalid) return;
    const val = this.userForm.value;
    const userObj = this.editingUser();

    if (userObj && userObj.id) {
      this.userRoleService.updateUser(userObj.id, val).subscribe(() => {
        this.loadUsers();
        this.showUserEditor.set(false);
      });
    } else {
      this.userRoleService.createUser(val).subscribe(() => {
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
