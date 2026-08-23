import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class FfStorageService {
  private platformId = inject(PLATFORM_ID);

  get<T>(key: string, fallback: T | null = null): T | null {
    if (!isPlatformBrowser(this.platformId)) return fallback;
    try {
      const raw = localStorage.getItem(key);
      return raw ? (JSON.parse(raw) as T) : fallback;
    } catch {
      return fallback;
    }
  }

  set(key: string, value: unknown): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(key, JSON.stringify(value));
  }

  remove(key: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.removeItem(key);
  }

  getString(key: string): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem(key);
  }

  setString(key: string, value: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(key, value);
  }
}
