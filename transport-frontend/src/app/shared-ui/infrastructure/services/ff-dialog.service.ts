import { Injectable, inject, Type } from '@angular/core';
import { MatDialog, MatDialogConfig, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import {
  FfConfirmDialogComponent,
  FfConfirmDialogData
} from '../../advanced/ff-confirm-dialog/ff-confirm-dialog.component';

export interface FfConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'primary' | 'danger' | 'warning';
}

@Injectable({ providedIn: 'root' })
export class FfDialogService {
  private dialog = inject(MatDialog);

  confirm(options: FfConfirmOptions): Observable<boolean> {
    const data: FfConfirmDialogData = {
      title: options.title ?? 'Confirm',
      message: options.message,
      confirmText: options.confirmText ?? 'Confirm',
      cancelText: options.cancelText ?? 'Cancel',
      type: options.type ?? 'primary'
    };
    const ref = this.dialog.open(FfConfirmDialogComponent, {
      width: '440px',
      panelClass: 'ff-dialog-panel',
      data
    });
    return ref.afterClosed() as Observable<boolean>;
  }

  open<T, R = unknown, D = unknown>(
    component: Type<T>,
    config?: MatDialogConfig<D>
  ): MatDialogRef<T, R> {
    return this.dialog.open(component, {
      panelClass: 'ff-dialog-panel',
      ...config
    });
  }
}
