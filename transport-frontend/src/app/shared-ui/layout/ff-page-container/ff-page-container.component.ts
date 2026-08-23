import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ff-page-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ff-page-container" [class.ff-page-container--fluid]="fluid()">
      <ng-content />
    </div>
  `,
  styles: [`
    .ff-page-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: var(--ff-space-6);
      height: 100%;
      min-height: 0;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      overflow: auto;
    }
    .ff-page-container--fluid { max-width: none; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfPageContainerComponent {
  readonly fluid = input<boolean>(false);
}
