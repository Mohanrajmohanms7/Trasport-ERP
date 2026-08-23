import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { FfThemeService } from './shared-ui/infrastructure/services/ff-theme.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: FfThemeService, useValue: { loadPersisted: () => undefined } }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should expose the application title signal', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app['title']()).toBe('transport-frontend');
  });
});
