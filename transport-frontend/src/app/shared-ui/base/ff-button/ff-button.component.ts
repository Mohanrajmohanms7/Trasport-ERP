import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FfButtonVariant, FfSize } from '../../infrastructure/enums/ff-size.enum';
import { FfPermissionService } from '../../infrastructure/services/ff-permission.service';

@Component({
  selector: 'ff-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-button.component.html',
  styleUrl: './ff-button.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfButtonComponent {
  private permissionService = inject(FfPermissionService);

  readonly label = input<string>('');
  readonly variant = input<FfButtonVariant>('primary');
  readonly size = input<FfSize>('md');
  readonly icon = input<string>('');
  readonly iconPosition = input<'start' | 'end'>('start');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly disabled = input<boolean>(false);
  readonly loading = input<boolean>(false);
  readonly fullWidth = input<boolean>(false);
  readonly permission = input<string>('');

  readonly ffClick = output<MouseEvent>();

  readonly visible = computed(() =>
    this.permissionService.can(this.permission() || undefined)
  );

  readonly isDisabled = computed(() => this.disabled() || this.loading());

  readonly classes = computed(() => {
    const parts = [
      'ff-btn',
      `ff-btn--${this.variant()}`,
      `ff-btn--${this.size()}`
    ];
    if (this.fullWidth()) parts.push('ff-btn--block');
    if (this.loading()) parts.push('ff-btn--loading');
    return parts.join(' ');
  });

  onClick(event: MouseEvent): void {
    if (this.isDisabled()) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    this.ffClick.emit(event);
  }
}
