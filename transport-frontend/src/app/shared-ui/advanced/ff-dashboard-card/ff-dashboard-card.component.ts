import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type FfDashboardCardColor = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';

@Component({
  selector: 'ff-dashboard-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-dashboard-card.component.html',
  styleUrl: './ff-dashboard-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfDashboardCardComponent {
  readonly label = input.required<string>();
  readonly value = input<string | number>('');
  readonly icon = input<string>('');
  readonly color = input<FfDashboardCardColor>('primary');
  readonly trend = input<string>('');
  readonly trendUp = input<boolean | null>(null);
}
