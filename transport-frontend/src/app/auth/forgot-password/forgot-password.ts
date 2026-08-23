import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatButtonModule, MatCardModule, MatInputModule, MatIconModule],
  templateUrl: './forgot-password.html',
  styles: []
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  // States
  loading = signal<boolean>(false);
  successMessage = signal<string>('');
  errorMessage = signal<string>('');

  forgotForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  onSubmit() {
    if (this.forgotForm.invalid) return;

    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    this.authService.forgotPassword(this.forgotForm.value.email).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.successMessage.set('Reset link sent to your registered email address!');
        } else {
          this.errorMessage.set(res.message || 'Forgot password request failed.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Error occurred. Please verify your email.');
      }
    });
  }
}
