import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ff-page-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="ff-page-footer" [class.ff-page-footer--sticky]="sticky()">
      <ng-content />
    </footer>
  `,
  styles: [`
    .ff-page-footer {
      padding: var(--ff-space-4) var(--ff-space-6);
      border-top: 1px solid var(--ff-border-default);
      background: var(--ff-surface-card);
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: var(--ff-space-3);
    }
    .ff-page-footer--sticky {
      position: sticky;
      bottom: 0;
      z-index: 10;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfPageFooterComponent {
  readonly sticky = input<boolean>(false);
}
