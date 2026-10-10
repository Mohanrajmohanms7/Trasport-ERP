import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent, ConfirmationData } from './confirmation-dialog';

/**
 * The one way to ask "are you sure?" in TransaFlow (never the browser's confirm()).
 * The action runs only when the user clicks the confirm button; Cancel, X, Esc do nothing,
 * and clicking the dark background is ignored.
 */
@Injectable({ providedIn: 'root' })
export class AppConfirmService {
  private dialog = inject(MatDialog);

  ask(data: ConfirmationData, action: () => void): void {
    this.dialog.open(ConfirmationDialogComponent, {
      data,
      width: '440px',
      maxWidth: 'calc(100vw - 32px)',
      disableClose: true,
      autoFocus: false
    }).afterClosed().subscribe(ok => { if (ok === true) action(); });
  }
}
