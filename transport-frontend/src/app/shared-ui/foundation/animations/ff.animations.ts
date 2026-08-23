import { trigger, transition, style, animate } from '@angular/animations';
import { FfAnimation } from '../tokens/layout.token';

export const ffFadeIn = trigger('ffFadeIn', [
  transition(':enter', [
    style({ opacity: 0 }),
    animate(FfAnimation.duration.normal, style({ opacity: 1 }))
  ])
]);

export const ffSlideInRight = trigger('ffSlideInRight', [
  transition(':enter', [
    style({ transform: 'translateX(100%)', opacity: 0 }),
    animate(
      `${FfAnimation.duration.normal} ${FfAnimation.easing.decelerate}`,
      style({ transform: 'translateX(0)', opacity: 1 })
    )
  ]),
  transition(':leave', [
    animate(
      `${FfAnimation.duration.fast} ${FfAnimation.easing.accelerate}`,
      style({ transform: 'translateX(100%)', opacity: 0 })
    )
  ])
]);

export const ffExpandCollapse = trigger('ffExpandCollapse', [
  transition(':enter', [
    style({ height: 0, opacity: 0, overflow: 'hidden' }),
    animate(
      `${FfAnimation.duration.normal} ${FfAnimation.easing.standard}`,
      style({ height: '*', opacity: 1 })
    )
  ]),
  transition(':leave', [
    style({ overflow: 'hidden' }),
    animate(
      `${FfAnimation.duration.fast} ${FfAnimation.easing.standard}`,
      style({ height: 0, opacity: 0 })
    )
  ])
]);
