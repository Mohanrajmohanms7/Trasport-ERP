import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { AuthService, UserProfile } from '../../services/auth.service';

/** Matches backend AuthService.validatePasswordPolicy */
function passwordPolicyValidator(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value || '');
  if (!value) return null;
  const errors: ValidationErrors = {};
  if (value.length < 8) errors['minlength'] = { requiredLength: 8, actualLength: value.length };
  if (!/[A-Z]/.test(value)) errors['upper'] = true;
  if (!/[a-z]/.test(value)) errors['lower'] = true;
  if (!/[0-9]/.test(value)) errors['digit'] = true;
  if (!/[^A-Za-z0-9]/.test(value)) errors['special'] = true;
  return Object.keys(errors).length ? errors : null;
}

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const newPassword = group.get('newPassword')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  if (!confirmPassword) return null;
  return newPassword === confirmPassword ? null : { mismatch: true };
}

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatCardModule, MatInputModule, MatIconModule, MatTabsModule],
  templateUrl: './profile.html',
  styles: [`
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes fadeInUp {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: translateY(0); }
    }
    @keyframes scaleIn {
      from { opacity: 0; transform: scale(0.95); }
      to { opacity: 1; transform: scale(1); }
    }
    .animate-fade-in {
      animation: fadeIn 0.25s ease forwards;
    }
    .animate-fade-in-up {
      animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
    .animate-scale-in {
      animation: scaleIn 0.45s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
    .profile-card {
      transition: box-shadow 0.3s ease, transform 0.3s ease, border-color 0.3s ease;
    }
    .profile-card:hover {
      box-shadow: 0 12px 30px -5px rgba(0, 0, 0, 0.08), 0 8px 16px -8px rgba(0, 0, 0, 0.08);
      transform: translateY(-2px);
    }
    .avatar-wrapper {
      position: relative;
      cursor: pointer;
    }
    .avatar-overlay {
      position: absolute;
      inset: 0;
      background: rgba(15, 23, 42, 0.65);
      opacity: 0;
      transition: opacity 0.2s ease;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: white;
    }
    .avatar-wrapper:hover .avatar-overlay {
      opacity: 1;
    }
  `]
})
export class UserProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  profile = signal<UserProfile | null>(null);
  loading = signal<boolean>(false);
  successMessage = signal<string>('');
  errorMessage = signal<string>('');

  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  ngOnInit() {
    this.initForms();
    this.loadProfile();
  }

  avatarBase64 = signal<string | null>(null);
  previewAvatar = signal<string | null>(null);

  initForms() {
    this.profileForm = this.fb.group({
      code: [{ value: '', disabled: true }],
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[a-zA-Z0-9._-]+$/)
      ]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      email: ['', [Validators.email, Validators.maxLength(100)]],
      phone: ['', [Validators.maxLength(20)]],
      bio: ['']
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, passwordPolicyValidator]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  loadProfile() {
    this.authService.getProfile().subscribe(res => {
      if (res.success && res.data) {
        this.profile.set(res.data);
        
        const desc = res.data.description || '';
        let avatar = '';
        let bio = '';
        if (desc.includes('|||')) {
          const parts = desc.split('|||');
          avatar = parts[0];
          bio = parts[1];
        } else if (desc.startsWith('data:image/')) {
          avatar = desc;
        } else {
          bio = desc;
        }
        
        this.avatarBase64.set(avatar || null);
        
        this.profileForm.patchValue({
          code: res.data.code,
          username: res.data.username,
          name: res.data.name,
          email: res.data.email,
          phone: res.data.phone,
          bio: bio
        });
      }
    });
  }

  saveProfile() {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const previousUsername = this.profile()?.username || '';
    
    // Construct final description from avatar and bio
    const avatarData = this.avatarBase64() || '';
    const bioData = this.profileForm.get('bio')?.value || '';
    const finalDescription = avatarData ? `${avatarData}|||${bioData}` : bioData;

    const profileData = this.profile();
    if (!profileData) {
      this.loading.set(false);
      return;
    }

    const formValues = this.profileForm.getRawValue();
    const payload: UserProfile = { 
      id: profileData.id,
      code: profileData.code,
      name: formValues.name,
      username: formValues.username,
      email: formValues.email,
      phone: formValues.phone,
      description: finalDescription,
      status: profileData.status,
      companyId: profileData.companyId,
      branchId: profileData.branchId
    };
    
    const usernameChanged = !!payload.username
      && payload.username.trim().toLowerCase() !== previousUsername.toLowerCase();

    this.authService.updateProfile(payload).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          if (usernameChanged) {
            this.successMessage.set('Username updated. Please sign in again with your new username.');
            setTimeout(() => {
              this.authService.clearLocalSession();
              this.router.navigate(['/login']);
            }, 1200);
            return;
          }
          this.successMessage.set('Profile details updated successfully!');
          this.loadProfile();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to update profile details.');
      }
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      // Limit file size to 200KB
      if (file.size > 200 * 1024) {
        this.errorMessage.set('Profile picture size must be under 200 KB');
        return;
      }

      const reader = new FileReader();
      reader.onload = () => {
        const base64String = reader.result as string;
        this.previewAvatar.set(base64String);
        input.value = ''; // Clear file input
      };
      reader.readAsDataURL(file);
    }
  }

  cancelPreview() {
    this.previewAvatar.set(null);
  }

  savePreview() {
    const img = this.previewAvatar();
    if (img) {
      this.avatarBase64.set(img);
      this.previewAvatar.set(null);
      this.saveProfile();
    }
  }

  removeAvatar() {
    this.avatarBase64.set(null);
    this.saveProfile();
  }

  changePassword() {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const { oldPassword, newPassword } = this.passwordForm.value;

    this.authService.changePassword({ oldPassword, newPassword }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.successMessage.set('Password changed successfully!');
          this.passwordForm.reset();
        } else {
          this.errorMessage.set(res.message || 'Password change failed.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Old password verification failed or invalid policy.');
      }
    });
  }

  passwordHint(): string {
    const ctrl = this.passwordForm?.get('newPassword');
    if (!ctrl || !ctrl.value || !ctrl.errors) return '';
    const parts: string[] = [];
    if (ctrl.errors['minlength']) parts.push('min 8 characters');
    if (ctrl.errors['upper']) parts.push('1 uppercase');
    if (ctrl.errors['lower']) parts.push('1 lowercase');
    if (ctrl.errors['digit']) parts.push('1 number');
    if (ctrl.errors['special']) parts.push('1 special (!@#...)');
    return parts.length ? `Password needs: ${parts.join(', ')}` : '';
  }
}
