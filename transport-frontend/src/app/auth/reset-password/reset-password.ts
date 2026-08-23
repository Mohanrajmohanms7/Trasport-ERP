import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatButtonModule, MatCardModule, MatInputModule, MatIconModule],
  templateUrl: './reset-password.html',
  styles: []
})
export class ResetPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // States
  hidePassword = signal<boolean>(true);
  loading = signal<boolean>(false);
  successMessage = signal<string>('');
  errorMessage = signal<string>('');

  resetForm: FormGroup = this.fb.group({
    username: ['', Validators.required],
    token: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: this.passwordMatchValidator });

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { 'mismatch': true };
  }

  onSubmit() {
    if (this.resetForm.invalid) return;

    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const { username, token, newPassword } = this.resetForm.value;

    this.authService.resetPassword({ username, token, newPassword }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.successMessage.set('Password reset successfully! Redirecting to sign in...');
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 3000);
        } else {
          this.errorMessage.set(res.message || 'Reset password failed.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Invalid username, token, or password policy.');
      }
    });
  }
}
