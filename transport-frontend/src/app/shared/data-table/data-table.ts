import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusPillComponent } from '../status-pill/status-pill';

export interface TableColumn {
  key: string;
  label: string;
  type?: 'text' | 'badge' | 'avatar' | 'actions' | 'currency';
  avatarKey?: string; // key for avatar image url
  subtitleKey?: string; // key for subtitle text under name
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, StatusPillComponent],
  templateUrl: './data-table.html',
  styles: []
})
export class DataTableComponent {
  @Input() columns: TableColumn[] = [];
  @Input() data: any[] = [];
  @Input() totalEntries: number = 0;
  @Input() showingStart: number = 0;
  @Input() showingEnd: number = 0;
  @Input() currentPage: number = 1;
  @Input() totalPages: number = 1;

  @Output() onRowClick = new EventEmitter<any>();
  @Output() onAction = new EventEmitter<{ action: 'view' | 'edit' | 'delete', row: any }>();
  @Output() onPageChange = new EventEmitter<number>();

  selectedRows = new Set<any>();

  toggleAll(event: any) {
    if (event.target.checked) {
      this.data.forEach(row => this.selectedRows.add(row));
    } else {
      this.selectedRows.clear();
    }
  }

  toggleRow(row: any, event: any) {
    if (event.target.checked) {
      this.selectedRows.add(row);
    } else {
      this.selectedRows.delete(row);
    }
  }

  isRowSelected(row: any): boolean {
    return this.selectedRows.has(row);
  }

  isAllSelected(): boolean {
    return this.data.length > 0 && this.selectedRows.size === this.data.length;
  }

  rowClick(row: any, event: MouseEvent) {
    // Avoid triggering row click if clicking checkbox or action button
    const target = event.target as HTMLElement;
    if (target.closest('input[type="checkbox"]') || target.closest('button')) {
      return;
    }
    this.onRowClick.emit(row);
  }

  triggerAction(action: 'view' | 'edit' | 'delete', row: any) {
    this.onAction.emit({ action, row });
  }

  changePage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.onPageChange.emit(page);
    }
  }
}
