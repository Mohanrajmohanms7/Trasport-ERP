import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FfGridActionEvent,
  FfGridColumn,
  FfGridConfig,
  FfPageEvent,
  FfSortEvent
} from '../../infrastructure/models/ff-config.interface';
import { FfCurrencyPipe, FfDateFormatPipe } from '../../infrastructure/pipes/ff.pipes';
import { FF_DEFAULT_PAGE_SIZE, FF_PAGE_SIZE_OPTIONS } from '../../infrastructure/constants/validation-messages';
import { ffTrackByField } from '../../infrastructure/utils/ff.utils';

@Component({
  selector: 'ff-grid',
  standalone: true,
  imports: [CommonModule, FfCurrencyPipe, FfDateFormatPipe],
  templateUrl: './ff-grid.component.html',
  styleUrl: './ff-grid.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfGridComponent<T extends object = Record<string, unknown>> {
  readonly config = input.required<FfGridConfig>();
  readonly data = input<T[]>([]);
  readonly loading = input<boolean>(false);

  readonly sortChange = output<FfSortEvent>();
  readonly pageChange = output<FfPageEvent>();
  readonly rowClick = output<T>();
  readonly action = output<FfGridActionEvent<T>>();

  readonly sortField = signal('');
  readonly sortDir = signal<'asc' | 'desc' | ''>('');
  readonly selected = signal<Set<unknown>>(new Set());

  readonly pageSizeOptions = FF_PAGE_SIZE_OPTIONS;

  readonly columns = computed(() => this.config().columns ?? []);
  readonly rowActions = computed(() => this.config().rowActions ?? []);
  readonly pageIndex = computed(() => this.config().pageIndex ?? 0);
  readonly pageSize = computed(() => this.config().pageSize ?? FF_DEFAULT_PAGE_SIZE);
  readonly totalRecords = computed(() => this.config().totalRecords ?? this.data().length);
  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalRecords() / this.pageSize()))
  );
  readonly showingStart = computed(() =>
    this.totalRecords() === 0 ? 0 : this.pageIndex() * this.pageSize() + 1
  );
  readonly showingEnd = computed(() =>
    Math.min((this.pageIndex() + 1) * this.pageSize(), this.totalRecords())
  );

  readonly trackBy = computed(() => {
    const field = this.config().trackByField;
    return field
      ? ffTrackByField<T>(field as keyof T)
      : (_i: number, row: T) => (row as { id?: unknown }).id ?? _i;
  });

  cellValue(row: T, col: FfGridColumn): unknown {
    return (row as Record<string, unknown>)[col.field];
  }

  asCurrency(value: unknown): number | string | null {
    if (value === null || value === undefined) return null;
    if (typeof value === 'number' || typeof value === 'string') return value;
    return null;
  }

  asDate(value: unknown): string | Date | null {
    if (value === null || value === undefined) return null;
    if (typeof value === 'string' || value instanceof Date) return value;
    return null;
  }

  asText(value: unknown): string {
    if (value === null || value === undefined) return '';
    return String(value);
  }

  badgeMeta(col: FfGridColumn, value: unknown): { color: string; label: string } {
    const key = String(value ?? '');
    const mapped = col.badgeMap?.[key];
    return {
      color: mapped?.color ?? 'neutral',
      label: mapped?.label ?? key
    };
  }

  onSort(col: FfGridColumn): void {
    if (!this.config().sortable || col.sortable === false) return;
    let dir: 'asc' | 'desc' | '' = 'asc';
    if (this.sortField() === col.field) {
      dir = this.sortDir() === 'asc' ? 'desc' : this.sortDir() === 'desc' ? '' : 'asc';
    }
    this.sortField.set(dir ? col.field : '');
    this.sortDir.set(dir);
    this.sortChange.emit({ field: col.field, direction: dir });
  }

  onRowClick(row: T): void {
    this.rowClick.emit(row);
  }

  onAction(actionId: string, row: T, event: Event): void {
    event.stopPropagation();
    this.action.emit({ action: actionId, row });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.pageChange.emit({ pageIndex: page, pageSize: this.pageSize() });
  }

  onPageSizeChange(event: Event): void {
    const size = Number((event.target as HTMLSelectElement).value);
    this.pageChange.emit({ pageIndex: 0, pageSize: size });
  }

  toggleSelect(row: T, event: Event): void {
    event.stopPropagation();
    const key = (row as { id?: unknown }).id ?? row;
    this.selected.update(set => {
      const next = new Set(set);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  isSelected(row: T): boolean {
    const key = (row as { id?: unknown }).id ?? row;
    return this.selected().has(key);
  }

  pages(): number[] {
    const total = this.totalPages();
    const current = this.pageIndex();
    const window = 5;
    let start = Math.max(0, current - Math.floor(window / 2));
    let end = Math.min(total, start + window);
    start = Math.max(0, end - window);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }
}
