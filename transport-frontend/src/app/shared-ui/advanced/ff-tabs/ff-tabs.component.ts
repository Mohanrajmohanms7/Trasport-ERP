import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface FfTabItem {
  id: string;
  label: string;
  icon?: string;
  disabled?: boolean;
}

@Component({
  selector: 'ff-tabs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-tabs.component.html',
  styleUrl: './ff-tabs.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfTabsComponent {
  readonly tabs = input.required<FfTabItem[]>();
  readonly activeTab = input<string>('');
  readonly variant = input<'line' | 'pills'>('line');

  readonly tabChange = output<string>();

  readonly internalActive = signal('');

  selectTab(id: string, disabled?: boolean): void {
    if (disabled) return;
    this.internalActive.set(id);
    this.tabChange.emit(id);
  }

  isActive(id: string): boolean {
    const explicit = this.activeTab();
    const active = explicit || this.internalActive() || this.tabs()[0]?.id;
    return active === id;
  }
}
