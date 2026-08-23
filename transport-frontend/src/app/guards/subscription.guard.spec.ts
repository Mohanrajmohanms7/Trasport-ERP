import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { subscriptionGuard } from './subscription.guard';
import { AuthService, LoginResponse } from '../services/auth.service';

describe('subscriptionGuard', () => {
  let currentUserValue: LoginResponse | null;
  let router: Router;

  beforeEach(() => {
    currentUserValue = null;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'renewal', children: [] },
          { path: '', children: [] }
        ]),
        {
          provide: AuthService,
          useValue: {
            currentUser: () => currentUserValue,
            logout: () => ({ subscribe: (h: { next?: () => void }) => h.next?.() })
          }
        }
      ]
    });
    router = TestBed.inject(Router);
  });

  it('allows SUPER_ADMIN even when subscriptionExpired is true', () => {
    currentUserValue = {
      token: 't',
      refreshToken: 'r',
      username: 'superadmin',
      name: 'SA',
      roles: ['SUPER_ADMIN'],
      subscriptionExpired: true
    };

    const result = TestBed.runInInjectionContext(() =>
      subscriptionGuard({} as never, { url: '/dashboard' } as never)
    );
    expect(result).toBeTrue();
  });

  it('does not throw when roles are missing', () => {
    currentUserValue = {
      token: 't',
      refreshToken: 'r',
      username: 'user',
      name: 'User',
      roles: undefined as unknown as string[]
    };

    expect(() =>
      TestBed.runInInjectionContext(() =>
        subscriptionGuard({} as never, { url: '/dashboard' } as never)
      )
    ).not.toThrow();
  });

  it('sends COMPANY_ADMIN to renewal when subscription expired', () => {
    const navigateSpy = spyOn(router, 'navigate');
    currentUserValue = {
      token: 't',
      refreshToken: 'r',
      username: 'admin',
      name: 'Admin',
      roles: ['COMPANY_ADMIN'],
      subscriptionExpired: true
    };

    const result = TestBed.runInInjectionContext(() =>
      subscriptionGuard({} as never, { url: '/dashboard' } as never)
    );
    expect(result).toBeFalse();
    expect(navigateSpy).toHaveBeenCalledWith(['/renewal']);
  });
});
