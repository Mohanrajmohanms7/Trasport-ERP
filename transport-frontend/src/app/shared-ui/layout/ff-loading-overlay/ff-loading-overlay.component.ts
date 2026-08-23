import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FfLoadingService } from '../../infrastructure/services/ff-loading.service';

@Component({
  selector: 'ff-loading-overlay',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (loading.loading()) {
      <div class="ff-overlay" role="status" aria-live="polite" aria-busy="true">
        <div class="ff-overlay__spinner"></div>
        @if (loading.message()) {
          <p class="ff-overlay__message">{{ loading.message() }}</p>
        }
      </div>
    }
  `,
  styles: [`
    .ff-overlay {
      position: fixed;
      inset: 0;
      z-index: 2000;
      background: rgba(15, 23, 42, 0.35);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: var(--ff-space-3);
    }
    .ff-overlay__spinner {
      width: 2.5rem;
      height: 2.5rem;
      border: 3px solid rgba(255,255,255,0.3);
      border-top-color: #fff;
      border-radius: 50%;
      animation: ff-spin 0.8s linear infinite;
    }
    .ff-overlay__message {
      margin: 0;
      color: #fff;
      font-size: var(--ff-text-sm);
      font-weight: 600;
    }
    @keyframes ff-spin { to { transform: rotate(360deg); } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfLoadingOverlayComponent {
  readonly loading = inject(FfLoadingService);
}
