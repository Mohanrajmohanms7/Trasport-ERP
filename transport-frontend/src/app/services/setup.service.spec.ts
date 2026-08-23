import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SetupService, SetupStatus } from './setup.service';

describe('SetupService', () => {
  let service: SetupService;
  let httpMock: HttpTestingController;

  const base: SetupStatus = {
    setupCompleted: false,
    hasBusinessData: false,
    companyCount: 0,
    branchCount: 0,
    vehicleCount: 0,
    driverCount: 0,
    customerCount: 0,
    materialCount: 0
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), SetupService]
    });
    service = TestBed.inject(SetupService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('needsSetup is true when incomplete and empty', () => {
    expect(service.needsSetup(base)).toBeTrue();
  });

  it('needsSetup is false when setup completed', () => {
    expect(service.needsSetup({ ...base, setupCompleted: true })).toBeFalse();
  });

  it('needsSetup is false when business data exists', () => {
    expect(service.needsSetup({ ...base, hasBusinessData: true })).toBeFalse();
  });

  it('getStatus stores successful payload', () => {
    service.getStatus().subscribe(res => {
      expect(res.success).toBeTrue();
      expect(service.status()?.companyCount).toBe(1);
    });

    const req = httpMock.expectOne('/api/v1/setup/status');
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'ok',
      data: { ...base, companyCount: 1 }
    });
  });
});
