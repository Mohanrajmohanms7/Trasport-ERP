import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { FfButtonComponent } from '../../base/ff-button/ff-button.component';

export interface FfConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'primary' | 'danger' | 'warning';
}

@Component({
  selector: 'ff-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, FfButtonComponent],
  templateUrl: './ff-confirm-dialog.component.html',
  styleUrl: './ff-confirm-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfConfirmDialogComponent {
  dialogRef = inject(MatDialogRef<FfConfirmDialogComponent, boolean>);
  data = inject<FfConfirmDialogData>(MAT_DIALOG_DATA);

  iconName(): string {
    switch (this.data.type) {
      case 'danger': return 'report_problem';
      case 'warning': return 'warning';
      default: return 'info';
    }
  }

  iconClass(): string {
    switch (this.data.type) {
      case 'danger': return 'ff-confirm__icon--danger';
      case 'warning': return 'ff-confirm__icon--warning';
      default: return 'ff-confirm__icon--primary';
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }
}
