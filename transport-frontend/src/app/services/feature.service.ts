import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';

export interface FeatureDef {
  code: string; parent: string | null; group: string; label: string; description: string;
  kind: 'MODULE' | 'TAB' | 'ACTION'; core: boolean; routes: string[]; tabKey: string | null;
}

/**
 * Subscription / client feature access on the screen side. The server enforces the same rules on every API call;
 * this service only hides menus, tabs and buttons the client's plan does not include.
 */
@Injectable({ providedIn: 'root' })
export class FeatureService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  readonly disabled = signal<Set<string>>(new Set());
  readonly catalog = signal<FeatureDef[]>([]);
  private loading: Promise<void> | null = null;
  private loadedFor: string | null = null;

  readonly isPlatformAdmin = computed(() => (this.auth.currentUser()?.roles || []).includes('SUPER_ADMIN'));

  /** Loads (once per login) the features switched off for this client. */
  load(force = false): Promise<void> {
    const who = localStorage.getItem('username') || '';
    if (!localStorage.getItem('token')) return Promise.resolve();
    if (!force && this.loading && this.loadedFor === who) return this.loading;
    this.loadedFor = who;
    this.loading = firstValueFrom(this.http.get<any>('/api/v1/auth/features'))
      .then(r => {
        this.disabled.set(new Set<string>(r?.data?.disabled ?? []));
        this.catalog.set(r?.data?.catalog ?? []);
      })
      .catch(() => { /* keep everything visible; the server still enforces */ });
    return this.loading;
  }

  reset(): void {
    this.loading = null;
    this.loadedFor = null;
    this.disabled.set(new Set());
  }

  /** True when the feature (and its parents) is included in the client's plan. */
  has(code: string): boolean {
    if (this.isPlatformAdmin()) return true;
    return !this.disabled().has(code);
  }

  /** Module that owns a screen URL (longest matching route), or null when not feature-controlled. */
  featureForUrl(url: string): FeatureDef | null {
    const path = (url || '').split('?')[0].split('#')[0];
    let best: FeatureDef | null = null;
    let len = -1;
    for (const f of this.catalog()) {
      for (const r of f.routes || []) {
        if ((path === r || path.startsWith(r + '/')) && r.length > len) { best = f; len = r.length; }
      }
    }
    return best;
  }

  routeAllowed(url: string): boolean {
    const f = this.featureForUrl(url);
    return !f || this.has(f.code);
  }
}
