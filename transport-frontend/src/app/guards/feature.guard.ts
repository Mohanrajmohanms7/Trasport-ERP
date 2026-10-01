import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { FeatureService } from '../services/feature.service';
import { FfNotificationService } from '../shared-ui/infrastructure/services/ff-notification.service';

/** Screens of modules the client's plan does not include cannot be opened by URL either. */
export const featureGuard: CanActivateChildFn = async (_route, state) => {
  const features = inject(FeatureService);
  const router = inject(Router);
  await features.load();
  if (features.routeAllowed(state.url)) return true;
  const f = features.featureForUrl(state.url);
  inject(FfNotificationService).error(`${f?.label || 'This screen'} is not included in your subscription.`);
  return router.parseUrl('/dashboard');
};
