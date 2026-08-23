import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type FfAvatarSize = 'sm' | 'md' | 'lg' | 'xl';

@Component({
  selector: 'ff-avatar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-avatar.component.html',
  styleUrl: './ff-avatar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfAvatarComponent {
  readonly name = input<string>('');
  readonly src = input<string>('');
  readonly alt = input<string>('');
  readonly size = input<FfAvatarSize>('md');
  readonly color = input<string>('');

  readonly initials = computed(() => {
    const n = this.name().trim();
    if (!n) return '?';
    const parts = n.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return n.substring(0, 2).toUpperCase();
  });

  readonly classes = computed(() => ['ff-avatar', `ff-avatar--${this.size()}`].join(' '));
}
