import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows detailed API error from errors array', () => {
    component.loginForm.setValue({ username: 'superadmin', password: 'bad' });
    authService.login.and.returnValue(
      throwError(() => ({
        error: {
          message: 'Validation or argument error',
          errors: ['Invalid username or password: bad creds']
        }
      }))
    );

    component.onSubmit();

    expect(component.errorMessage()).toContain('Invalid username or password');
  });

  it('routes SUPER_ADMIN to platform-admin on success', () => {
    const routerNavigate = spyOn(component['router'], 'navigate');
    component.loginForm.setValue({ username: 'superadmin', password: 'Super@123' });
    authService.login.and.returnValue(
      of({
        success: true,
        message: 'ok',
        data: {
          token: 't',
          refreshToken: 'r',
          username: 'superadmin',
          name: 'SA',
          roles: ['SUPER_ADMIN']
        }
      })
    );

    component.onSubmit();

    expect(routerNavigate).toHaveBeenCalledWith(['/platform-admin']);
  });
});
