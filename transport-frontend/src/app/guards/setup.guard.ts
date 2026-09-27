import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { SetupService } from '../services/setup.service';

/**
 * Sends a company admin to the Setup Wizard only for a company that was never provisioned
 * (no SETUP_COMPLETED flag and no business data) — e.g. a fresh self-hosted install.
 * Super Admin skips company ERP setup (uses Platform Admin instead).
 * Any failure to reach the status endpoint lets navigation through, so a
 * backend hiccup can never lock the user out of the application.
 */
export const setupGuard: CanActivateFn = () => {
  const setupService = inject(SetupService);
  const authService = inject(AuthService);
  const router = inject(Router);

  const roles = authService.currentUser()?.roles || [];
  if (roles.includes('SUPER_ADMIN')) {
    return true;
  }
  // Only a company admin can run the wizard; everyone else always goes straight to the app.
  // Clients onboarded by Platform Admin are marked set up, so their admin is not redirected either.
  if (!roles.includes('COMPANY_ADMIN') && !roles.includes('ADMIN')) {
    return true;
  }

  return setupService.getStatus().pipe(
    map(res => {
      if (res.success && res.data && setupService.needsSetup(res.data)) {
        return router.createUrlTree(['/setup']);
      }
      return true;
    }),
    catchError(() => of(true))
  );
};
