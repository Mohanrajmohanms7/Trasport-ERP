import { Injectable, signal } from '@angular/core';

export type FfNotificationType = 'success' | 'error' | 'warning' | 'info';

export interface FfNotification {
  id: number;
  type: FfNotificationType;
  message: string;
  duration?: number;
}

@Injectable({ providedIn: 'root' })
export class FfNotificationService {
  private nextId = 1;
  readonly notifications = signal<FfNotification[]>([]);

  success(message: string, duration = 4000): void {
    this.push('success', message, duration);
  }

  error(message: string, duration = 6000): void {
    this.push('error', message, duration);
  }

  warning(message: string, duration = 5000): void {
    this.push('warning', message, duration);
  }

  info(message: string, duration = 4000): void {
    this.push('info', message, duration);
  }

  dismiss(id: number): void {
    this.notifications.update(list => list.filter(n => n.id !== id));
  }

  private push(type: FfNotificationType, message: string, duration: number): void {
    const id = this.nextId++;
    this.notifications.update(list => [...list, { id, type, message, duration }]);
    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
  }
}
