import { Component, HostBinding, OnInit, ViewEncapsulation, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmationData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'primary' | 'danger' | 'warning';
  /** Material icon on the confirm button (default: delete for danger, check otherwise). */
  confirmIcon?: string;
}

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule],
  templateUrl: './confirmation-dialog.html',
  styleUrl: './confirmation-dialog.css',
  encapsulation: ViewEncapsulation.None
})
export class ConfirmationDialogComponent implements OnInit {
  dialogRef = inject(MatDialogRef<ConfirmationDialogComponent>);
  data = inject<ConfirmationData>(MAT_DIALOG_DATA);

  @HostBinding('class.ff-confirm-host')
  readonly hostClass = true;

  @HostBinding('class.ff-confirm-host--dark')
  isDark = false;

  get tone(): 'primary' | 'danger' | 'warning' {
    return this.data.type || 'primary';
  }

  get iconName(): string {
    switch (this.tone) {
      case 'danger':
        return 'delete_forever';
      case 'warning':
        return 'warning';
      default:
        return 'help';
    }
  }

  get confirmLabel(): string {
    return this.data.confirmText || (this.tone === 'danger' ? 'Delete' : 'Confirm');
  }

  get confirmIconName(): string {
    return this.data.confirmIcon || (this.tone === 'danger' ? 'delete' : 'check');
  }

  get cancelLabel(): string {
    return this.data.cancelText || 'Cancel';
  }

  ngOnInit(): void {
    const root = document.documentElement;
    this.isDark =
      root.classList.contains('dark') ||
      root.getAttribute('data-ff-theme') === 'dark' ||
      root.getAttribute('data-theme') === 'dark';
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }
}
