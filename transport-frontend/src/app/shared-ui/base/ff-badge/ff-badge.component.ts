import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type FfBadgeColor = 'primary' | 'success' | 'danger' | 'warning' | 'info' | 'neutral';

@Component({
  selector: 'ff-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-badge.component.html',
  styleUrl: './ff-badge.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfBadgeComponent {
  readonly label = input.required<string>();
  readonly color = input<FfBadgeColor>('neutral');
  readonly dot = input<boolean>(false);
  readonly size = input<'sm' | 'md'>('md');

  readonly classes = computed(() =>
    ['ff-badge', `ff-badge--${this.color()}`, `ff-badge--${this.size()}`].join(' ')
  );
}
