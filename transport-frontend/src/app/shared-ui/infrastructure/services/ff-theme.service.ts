import { Injectable, signal, computed, effect, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FfThemeMode } from '../../foundation/themes/theme.contract';
import { FF_THEME_STORAGE_KEY, FF_CONTRAST_STORAGE_KEY } from '../constants/validation-messages';

@Injectable({ providedIn: 'root' })
export class FfThemeService {
  private platformId = inject(PLATFORM_ID);

  readonly mode = signal<FfThemeMode>('light');
  readonly highContrast = signal(false);
  readonly isDark = computed(() => this.mode() === 'dark');

  constructor() {
    effect(() => {
      this.applyToDom(this.mode(), this.highContrast());
    });
  }

  loadPersisted(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // Prefer ff-theme, fall back to legacy theme key, then OS preference
    const ffStored = localStorage.getItem(FF_THEME_STORAGE_KEY);
    const legacy = localStorage.getItem('theme');
    const resolved =
      ffStored === 'light' || ffStored === 'dark'
        ? ffStored
        : legacy === 'light' || legacy === 'dark'
          ? legacy
          : window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light';

    this.mode.set(resolved as FfThemeMode);
    // Keep both keys aligned
    localStorage.setItem(FF_THEME_STORAGE_KEY, resolved);
    localStorage.setItem('theme', resolved);

    const contrast = localStorage.getItem(FF_CONTRAST_STORAGE_KEY) === 'true';
    this.highContrast.set(contrast);
  }

  setTheme(mode: FfThemeMode): void {
    this.mode.set(mode);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(FF_THEME_STORAGE_KEY, mode);
      localStorage.setItem('theme', mode);
    }
  }

  toggleTheme(): void {
    this.setTheme(this.mode() === 'light' ? 'dark' : 'light');
  }

  setHighContrast(enabled: boolean): void {
    this.highContrast.set(enabled);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(FF_CONTRAST_STORAGE_KEY, String(enabled));
    }
  }

  private applyToDom(mode: FfThemeMode, contrast: boolean): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const root = document.documentElement;
    root.setAttribute('data-ff-theme', mode);
    root.classList.toggle('dark', mode === 'dark');
    if (contrast) {
      root.setAttribute('data-ff-contrast', 'high');
    } else {
      root.removeAttribute('data-ff-contrast');
    }
  }
}
