import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  forwardRef,
  inject,
  input,
  output,
  signal
} from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';
import { FfSelectOption } from '../../infrastructure/models/ff-config.interface';

@Component({
  selector: 'ff-dropdown',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-dropdown.component.html',
  styleUrl: './ff-dropdown.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfDropdownComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfDropdownComponent), multi: true }
  ],
  host: {
    class: 'ff-dropdown-host'
  }
})
export class FfDropdownComponent extends FfControlBase<unknown> {
  private host = inject(ElementRef<HTMLElement>);

  readonly options = input<FfSelectOption[]>([]);
  readonly emptyLabel = input<string>('Select…');
  readonly searchable = input<boolean>(true);
  readonly clearable = input<boolean>(true);
  readonly searchPlaceholder = input<string>('Search options...');
  readonly compareWith = input<(a: unknown, b: unknown) => boolean>((a, b) => a === b);
  readonly outlined = input<boolean>(true);

  /** Emitted when user clicks the refresh icon in the panel */
  readonly ffRefresh = output<void>();

  readonly panelOpen = signal(false);
  readonly query = signal('');
  readonly activeIndex = signal(-1);
  /** Fixed-position panel so it escapes modal overflow clipping */
  readonly panelStyle = signal<Record<string, string>>({});

  readonly selectedOption = computed(() => {
    const current = this.value();
    if (current === null || current === undefined || current === '') return null;
    return this.options().find(o => this.compareWith()(o.value, current)) ?? null;
  });

  readonly displayText = computed(() => this.selectedOption()?.label ?? '');

  readonly hasValue = computed(() => this.selectedOption() !== null);

  readonly filteredOptions = computed(() => {
    const q = this.query().trim().toLowerCase();
    const opts = this.options();
    if (!q) return opts;
    return opts.filter(o => o.label.toLowerCase().includes(q));
  });

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.panelOpen()) return;
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.closePanel();
    }
  }

  @HostListener('window:resize')
  @HostListener('window:scroll')
  onViewportChange(): void {
    if (this.panelOpen()) this.repositionPanel();
  }

  togglePanel(): void {
    if (this.isDisabled() || this.isReadonly()) return;
    if (this.panelOpen()) {
      this.closePanel();
    } else {
      this.openPanel();
    }
  }

  openPanel(): void {
    if (this.isDisabled() || this.isReadonly()) return;
    this.panelOpen.set(true);
    this.query.set('');
    const idx = this.filteredOptions().findIndex(o =>
      this.compareWith()(o.value, this.value())
    );
    this.activeIndex.set(idx >= 0 ? idx : 0);
    this.onFocus();
    queueMicrotask(() => {
      this.repositionPanel();
      const input = this.host.nativeElement.querySelector('.ff-dd__search-input') as HTMLInputElement | null;
      input?.focus();
    });
  }

  closePanel(): void {
    if (!this.panelOpen()) return;
    this.panelOpen.set(false);
    this.query.set('');
    this.panelStyle.set({});
    this.onBlur();
  }

  private repositionPanel(): void {
    const trigger = this.host.nativeElement.querySelector('.ff-dd__trigger') as HTMLElement | null;
    if (!trigger) return;

    const rect = trigger.getBoundingClientRect();
    const gap = 6;
    const viewportPad = 8;
    const estimatedPanelH = Math.min(320, 56 + this.filteredOptions().length * 44);
    const spaceBelow = window.innerHeight - rect.bottom - viewportPad;
    const spaceAbove = rect.top - viewportPad;
    const openUpward = spaceBelow < estimatedPanelH && spaceAbove > spaceBelow;
    const maxH = Math.max(160, Math.min(320, openUpward ? spaceAbove - gap : spaceBelow - gap));

    if (openUpward) {
      this.panelStyle.set({
        position: 'fixed',
        top: 'auto',
        bottom: `${window.innerHeight - rect.top + gap}px`,
        left: `${rect.left}px`,
        width: `${rect.width}px`,
        right: 'auto',
        maxHeight: `${maxH}px`,
        zIndex: '10050'
      });
    } else {
      this.panelStyle.set({
        position: 'fixed',
        top: `${rect.bottom + gap}px`,
        bottom: 'auto',
        left: `${rect.left}px`,
        width: `${rect.width}px`,
        right: 'auto',
        maxHeight: `${maxH}px`,
        zIndex: '10050'
      });
    }
  }

  onSearchInput(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
    this.activeIndex.set(0);
  }

  selectOption(opt: FfSelectOption): void {
    if (opt.disabled) return;
    this.emitValue(opt.value);
    this.panelOpen.set(false);
    this.query.set('');
    this.markTouched();
  }

  clear(event: Event): void {
    event.stopPropagation();
    if (this.isDisabled() || this.isReadonly() || !this.clearable()) return;
    this.emitValue(null);
    this.markTouched();
  }

  refresh(event: Event): void {
    event.stopPropagation();
    this.query.set('');
    this.ffRefresh.emit();
  }

  isSelected(opt: FfSelectOption): boolean {
    return this.compareWith()(opt.value, this.value());
  }

  onTriggerKeydown(event: KeyboardEvent): void {
    if (this.isDisabled() || this.isReadonly()) return;

    switch (event.key) {
      case 'Enter':
      case ' ':
      case 'ArrowDown':
        event.preventDefault();
        if (!this.panelOpen()) this.openPanel();
        break;
      case 'Escape':
        if (this.panelOpen()) {
          event.preventDefault();
          this.closePanel();
        }
        break;
    }
  }

  onPanelKeydown(event: KeyboardEvent): void {
    const opts = this.filteredOptions().filter(o => !o.disabled);
    if (!opts.length) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeIndex.update(i => Math.min(i + 1, opts.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex.update(i => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const idx = this.activeIndex();
      const visible = this.filteredOptions();
      if (idx >= 0 && visible[idx] && !visible[idx].disabled) {
        this.selectOption(visible[idx]);
      }
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.closePanel();
    }
  }
}
