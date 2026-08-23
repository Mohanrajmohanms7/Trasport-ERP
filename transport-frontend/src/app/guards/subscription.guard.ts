import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const subscriptionGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const currentUser = authService.currentUser();
  if (currentUser) {
    const roles = currentUser.roles || [];
    const isSuperAdmin = roles.includes('SUPER_ADMIN');
    if (isSuperAdmin) {
      return true;
    }

    if (currentUser.subscriptionExpired) {
      if (state.url.includes('/renewal')) {
        return true;
      }
      
      const isCompanyAdmin = roles.includes('COMPANY_ADMIN');
      if (isCompanyAdmin) {
        router.navigate(['/renewal']);
      } else {
        authService.logout().subscribe(() => {
          router.navigate(['/login'], { queryParams: { expired: 'true' } });
        });
      }
      return false;
    }
  }

  if (state.url.includes('/renewal')) {
    router.navigate(['/']);
    return false;
  }

  return true;
};
