import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { FfBusinessRule, FfBusinessRuleEffect } from '../models/ff-config.interface';

@Injectable({ providedIn: 'root' })
export class FfPermissionService {
  private auth = inject(AuthService);

  can(permission: string | undefined): boolean {
    if (!permission) return true;
    const user = this.auth.currentUser();
    if (!user) return false;
    if (user.roles?.includes('SUPER_ADMIN')) return true;
    return user.roles?.some(r => r === permission || r.endsWith(permission)) ?? false;
  }

  hasRole(role: string | string[]): boolean {
    const roles = Array.isArray(role) ? role : [role];
    const user = this.auth.currentUser();
    if (!user?.roles?.length) return false;
    if (user.roles.includes('SUPER_ADMIN')) return true;
    return roles.some(r => user.roles.includes(r));
  }
}

/**
 * In-memory business rule registry. Rules can be registered at startup
 * or loaded from API later without changing component APIs.
 */
@Injectable({ providedIn: 'root' })
export class FfBusinessRuleService {
  private rules = new Map<string, FfBusinessRule>();

  register(rule: FfBusinessRule): void {
    this.rules.set(rule.id, rule);
  }

  registerAll(rules: FfBusinessRule[]): void {
    rules.forEach(r => this.register(r));
  }

  evaluate(ruleId: string | undefined): FfBusinessRuleEffect {
    if (!ruleId) return {};
    return this.rules.get(ruleId)?.effects ?? {};
  }

  get(ruleId: string): FfBusinessRule | undefined {
    return this.rules.get(ruleId);
  }
}
