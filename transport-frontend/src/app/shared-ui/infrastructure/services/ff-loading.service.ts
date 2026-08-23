import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class FfLoadingService {
  private counter = 0;
  readonly loading = signal(false);
  readonly message = signal<string>('');

  show(message = ''): void {
    this.counter++;
    this.message.set(message);
    this.loading.set(true);
  }

  hide(): void {
    this.counter = Math.max(0, this.counter - 1);
    if (this.counter === 0) {
      this.loading.set(false);
      this.message.set('');
    }
  }

  reset(): void {
    this.counter = 0;
    this.loading.set(false);
    this.message.set('');
  }
}
