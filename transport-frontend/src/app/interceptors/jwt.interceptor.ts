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
      if (err.status === 403 && err.error?.message === 'SUBSCRIPTION_EXPIRED') {
        authService.setSubscriptionExpired(true);
        router.navigate(['/renewal']);
      }
      return throwError(() => err);
    })
  );
};
