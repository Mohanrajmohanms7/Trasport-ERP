import { FfNotificationService } from '../shared-ui/infrastructure/services/ff-notification.service';
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, throwError } from 'rxjs';

const PUBLIC_AUTH_URLS = [
  '/api/v1/auth/login',
  '/api/v1/auth/refresh',
  '/api/v1/auth/forgot-password',
  '/api/v1/auth/reset-password'
];

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notify = inject(FfNotificationService);

  const isPublicAuth = PUBLIC_AUTH_URLS.some(url => req.url.includes(url));
  if (isPublicAuth) {
    // Never send a stale Bearer token on login/refresh — it can cause 403 from security filters
    const cleaned = req.clone({
      headers: req.headers.delete('Authorization')
    });
    return next(cleaned);
  }

  const token = localStorage.getItem('token');
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError(err => {
      if (err.status === 403 && err.error?.message === 'FEATURE_DISABLED') {
        notify.error(err.error?.errors?.[0] || 'This feature is not included in your subscription.');
      }
      if (err.status === 403 && err.error?.message === 'PASSWORD_CHANGE_REQUIRED') {
        authService.setPasswordChangeRequired(true);
        router.navigate(['/profile'], { queryParams: { changePassword: 1 } });
      }
      if (err.status === 403 && err.error?.message === 'SUBSCRIPTION_EXPIRED') {
        authService.setSubscriptionExpired(true);
        router.navigate(['/renewal']);
      }
      return throwError(() => err);
    })
  );
};
