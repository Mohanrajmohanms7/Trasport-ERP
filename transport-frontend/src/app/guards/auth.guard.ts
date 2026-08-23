import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    const roles = authService.currentUser()?.roles || [];
    const isSuperAdmin = roles.includes('SUPER_ADMIN');
    const url = state.url;

    if (isSuperAdmin) {
      // Super Admin is only allowed on /platform-admin and /profile
      if (!url.startsWith('/platform-admin') && !url.startsWith('/profile')) {
        router.navigate(['/platform-admin']);
        return false;
      }
    } else {
      // Tenant users are not allowed on /platform-admin
      if (url.startsWith('/platform-admin')) {
        router.navigate(['/dashboard']);
        return false;
      }
    }
    return true;
  }

  // Not authenticated, redirect to login
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
