import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatButtonModule, MatCardModule, MatInputModule, MatIconModule],
  templateUrl: './login.html',
  styles: []
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // States
  hidePassword = signal<boolean>(true);
  loading = signal<boolean>(false);
  errorMessage = signal<string>('');

  loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.maxLength(50)]],
    password: ['', [Validators.required]]
  });


  onSubmit() {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          const roles = res.data?.roles || [];
          if (roles.includes('SUPER_ADMIN')) {
            this.router.navigate(['/platform-admin']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        } else {
          this.errorMessage.set(res.message || 'Login failed');
        }
      },
      error: (err) => {
        this.loading.set(false);
        const detail = err.error?.errors?.[0] || err.error?.message;
        this.errorMessage.set(detail || 'Invalid username or password');
      }
    });
  }
}
