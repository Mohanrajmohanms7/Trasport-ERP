import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FfNotificationService } from '../../infrastructure/services/ff-notification.service';

@Component({
  selector: 'ff-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ff-toast-host" aria-live="polite">
      @for (n of notify.notifications(); track n.id) {
        <div class="ff-toast" [attr.data-type]="n.type">
          <span class="ff-toast__msg">{{ n.message }}</span>
          <button type="button" class="ff-toast__close" aria-label="Dismiss" (click)="notify.dismiss(n.id)">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .ff-toast-host {
      position: fixed;
      top: 1rem;
      right: 1rem;
      z-index: 3000;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 360px;
    }
    .ff-toast {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-radius: var(--ff-radius-lg);
      box-shadow: var(--ff-shadow-lg);
      background: var(--ff-surface-card);
      border: 1px solid var(--ff-border-default);
      color: var(--ff-text-primary);
      font-size: var(--ff-text-sm);
      font-weight: 600;
    }
    .ff-toast[data-type='success'] { border-left: 4px solid var(--ff-color-success-500); }
    .ff-toast[data-type='error'] { border-left: 4px solid var(--ff-color-danger-500); }
    .ff-toast[data-type='warning'] { border-left: 4px solid var(--ff-color-warning-500); }
    .ff-toast[data-type='info'] { border-left: 4px solid var(--ff-color-info-500); }
    .ff-toast__msg { flex: 1; }
    .ff-toast__close {
      background: none; border: none; cursor: pointer; color: var(--ff-text-muted);
      display: flex; padding: 0;
    }
    .ff-toast__close .material-symbols-outlined { font-size: 1.125rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfToastComponent {
  readonly notify = inject(FfNotificationService);
}
