import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { Observable, of, throwError, isObservable } from 'rxjs';
import { setupGuard } from './setup.guard';
import { AuthService, LoginResponse } from '../services/auth.service';
import { SetupService } from '../services/setup.service';

describe('setupGuard', () => {
  let currentUserValue: LoginResponse | null;
  let setupService: jasmine.SpyObj<SetupService>;

  beforeEach(() => {
    currentUserValue = null;
    setupService = jasmine.createSpyObj('SetupService', ['getStatus', 'needsSetup']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'setup', children: [] }]),
        {
          provide: AuthService,
          useValue: {
            currentUser: () => currentUserValue
          }
        },
        { provide: SetupService, useValue: setupService }
      ]
    });
  });

  function runGuard(): unknown {
    return TestBed.runInInjectionContext(() => setupGuard({} as never, {} as never));
  }

  it('allows SUPER_ADMIN without calling setup status', () => {
    currentUserValue = {
      token: 't',
      refreshToken: 'r',
      username: 'superadmin',
      name: 'SA',
      roles: ['SUPER_ADMIN']
    };

    expect(runGuard()).toBeTrue();
    expect(setupService.getStatus).not.toHaveBeenCalled();
  });

  it('redirects company users to /setup when setup is needed', (done) => {
    currentUserValue = {
      token: 't',
      refreshToken: 'r',
      username: 'admin',
      name: 'Admin',
      roles: ['COMPANY_ADMIN']
    };
    setupService.getStatus.and.returnValue(
      of({
        success: true,
        message: 'ok',
        data: {
          setupCompleted: false,
          hasBusinessData: false,
          companyCount: 1,
          branchCount: 1,
          vehicleCount: 0,
          driverCount: 0,
          customerCount: 0,
          materialCount: 0
        }
      })
    );
    setupService.needsSetup.and.returnValue(true);

    const result = runGuard();
    expect(isObservable(result)).toBeTrue();
    (result as Observable<boolean | UrlTree>).subscribe((value) => {
      expect(value instanceof UrlTree).toBeTrue();
      expect(TestBed.inject(Router).serializeUrl(value as UrlTree)).toContain('setup');
      done();
    });
  });

  it('allows navigation when setup status call fails', (done) => {
    currentUserValue = {
      token: 't',
      refreshToken: 'r',
      username: 'admin',
      name: 'Admin',
      roles: ['COMPANY_ADMIN']
    };
    setupService.getStatus.and.returnValue(throwError(() => new Error('network')));

    const result = runGuard();
    (result as Observable<boolean | UrlTree>).subscribe((value) => {
      expect(value).toBeTrue();
      done();
    });
  });
});
