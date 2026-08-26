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
          @if (n.type === 'success') {
            <span class="material-symbols-outlined ff-toast__icon">check_circle</span>
          } @else if (n.type === 'error') {
            <span class="material-symbols-outlined ff-toast__icon">error</span>
          } @else if (n.type === 'warning') {
            <span class="material-symbols-outlined ff-toast__icon">warning</span>
          } @else {
            <span class="material-symbols-outlined ff-toast__icon">info</span>
          }
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
      top: 1.5rem;
      right: 1.5rem;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      max-width: 400px;
    }
    .ff-toast {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      padding: 0.85rem 1.15rem;
      border-radius: var(--ff-radius-lg);
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
      border: 1.5px solid var(--ff-border-default);
      font-size: var(--ff-text-sm);
      font-weight: 600;
      min-width: 320px;
      transition: all 0.2s ease-in-out;
    }
    .ff-toast__icon {
      font-size: 1.25rem;
      flex-shrink: 0;
    }
    .ff-toast__msg {
      flex: 1;
      line-height: 1.4;
    }
    .ff-toast__close {
      background: none;
      border: none;
      cursor: pointer;
      color: inherit;
      display: inline-flex;
      padding: 0.25rem;
      margin: -0.25rem;
      opacity: 0.6;
      transition: opacity 0.2s;
    }
    .ff-toast__close:hover {
      opacity: 1;
    }
    .ff-toast__close .material-symbols-outlined {
      font-size: 1.125rem;
    }

    /* Light Theme Theming */
    .ff-toast[data-type='success'] {
      background: #f0fdf4;
      color: #14532d;
      border-color: #bbf7d0;
      border-left: 4px solid #22c55e;
    }
    .ff-toast[data-type='success'] .ff-toast__icon {
      color: #22c55e;
    }

    .ff-toast[data-type='error'] {
      background: #fef2f2;
      color: #7f1d1d;
      border-color: #fca5a5;
      border-left: 4px solid #ef4444;
    }
    .ff-toast[data-type='error'] .ff-toast__icon {
      color: #ef4444;
    }

    .ff-toast[data-type='warning'] {
      background: #fefce8;
      color: #713f12;
      border-color: #fef08a;
      border-left: 4px solid #eab308;
    }
    .ff-toast[data-type='warning'] .ff-toast__icon {
      color: #eab308;
    }

    .ff-toast[data-type='info'] {
      background: #eff6ff;
      color: #1e3a8a;
      border-color: #bfdbfe;
      border-left: 4px solid #3b82f6;
    }
    .ff-toast[data-type='info'] .ff-toast__icon {
      color: #3b82f6;
    }

    /* Dark Theme Theming */
    :host-context(.dark) .ff-toast[data-type='success'] {
      background: #052e16;
      color: #86efac;
      border-color: #14532d;
    }
    :host-context(.dark) .ff-toast[data-type='error'] {
      background: #450a0a;
      color: #fca5a5;
      border-color: #7f1d1d;
    }
    :host-context(.dark) .ff-toast[data-type='warning'] {
      background: #422006;
      color: #fde047;
      border-color: #713f12;
    }
    :host-context(.dark) .ff-toast[data-type='info'] {
      background: #172554;
      color: #93c5fd;
      border-color: #1e3a8a;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfToastComponent {
  readonly notify = inject(FfNotificationService);
}
