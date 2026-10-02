import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { jwtInterceptor } from './interceptors/jwt.interceptor';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    // Dialogs close only through their own buttons (Cancel / Close / Save) — not by clicking outside or pressing Esc.
    { provide: MAT_DIALOG_DEFAULT_OPTIONS, useValue: { disableClose: true, hasBackdrop: true, autoFocus: 'first-tabbable', restoreFocus: true } },
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimationsAsync()
  ]
};
