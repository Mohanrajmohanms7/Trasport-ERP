import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type FfStatusColor = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'primary';

@Component({
  selector: 'ff-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="ff-status" [attr.data-color]="color()" [class.ff-status--dot]="dot()">
      @if (dot()) { <span class="ff-status__dot" aria-hidden="true"></span> }
      {{ label() }}
    </span>
  `,
  styles: [`
    .ff-status {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 10px;
      border-radius: var(--ff-radius-full);
      font-size: var(--ff-text-xs);
      font-weight: 600;
      background: var(--ff-surface-hover);
      color: var(--ff-text-secondary);
    }
    .ff-status[data-color='success'] { background: var(--ff-color-success-50); color: var(--ff-color-success-700); }
    .ff-status[data-color='danger'] { background: var(--ff-color-danger-50); color: var(--ff-color-danger-700); }
    .ff-status[data-color='warning'] { background: var(--ff-color-warning-50); color: var(--ff-color-warning-700); }
    .ff-status[data-color='info'], .ff-status[data-color='primary'] { background: var(--ff-color-primary-50); color: var(--ff-color-primary-700); }
    .ff-status__dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfStatusBadgeComponent {
  readonly label = input.required<string>();
  readonly color = input<FfStatusColor>('neutral');
  readonly dot = input<boolean>(true);
}
