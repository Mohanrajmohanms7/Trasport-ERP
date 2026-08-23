import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MobilityMgmtService, GpsTracking, AiPrediction } from '../../services/mobility-mgmt.service';
import { MasterService } from '../../services/master.service';
import { PlatformAdminService } from '../../services/platform-admin.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { FfDropdownComponent, FfSelectOption, FfNotificationService, FfToastComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

@Component({
  selector: 'app-mobility-details-console',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatTabsModule, MatCardModule, MatButtonModule, FfDropdownComponent, FfToastComponent],
  templateUrl: './mobility-details-console.html',
  styles: []
})
export class MobilityDetailsConsoleComponent implements OnInit {
  private mobilityMgmtService = inject(MobilityMgmtService);
  private masterService = inject(MasterService);
  private platformService = inject(PlatformAdminService);
  private notify = inject(FfNotificationService);

  activeTab = signal<string>('gps');
  loading = signal<boolean>(false);
  statusMessage = signal<string>('');

  gpsPings = signal<GpsTracking[]>([]);
  aiPredictions = signal<AiPrediction[]>([]);
  vehicles = signal<any[]>([]);

  selectedVehicleId = signal<number | null>(null);
  selectedVehicleControl = new FormControl<number | null>(null);
  get vehicleOptions(): FfSelectOption[] {
    return this.vehicles().map(vehicle => ({
      label: vehicle.registrationNumber || vehicle.code || vehicle.name || `Vehicle #${vehicle.id}`,
      value: vehicle.id
    }));
  }

  backupHistory = signal<any[]>([]);

  private companyId = resolveTenantCompanyId();

  ngOnInit() {
    this.selectedVehicleControl.valueChanges.subscribe(vehicleId => {
      if (vehicleId != null) this.onVehicleChange(vehicleId);
    });
    this.loadVehicles();
    this.loadAi();
    this.loadBackupHistory();
  }

  loadVehicles() {
    this.masterService.getMasters<any>('vehicles', this.companyId, { size: 100, page: 0 }).subscribe({
      next: (res) => {
        const list = res.success ? (res.data?.content || res.data || []) : [];
        this.vehicles.set(Array.isArray(list) ? list : []);
        if (this.vehicles().length && this.selectedVehicleId() == null) {
          const firstId = this.vehicles()[0].id;
          this.selectedVehicleId.set(firstId);
          this.selectedVehicleControl.setValue(firstId, { emitEvent: false });
          this.loadGps();
        }
      },
      error: () => {
        this.vehicles.set([]);
        this.notify.warning('Could not load fleet vehicles for GPS tracking.');
      }
    });
  }

  loadGps() {
    const vehicleId = this.selectedVehicleId();
    if (vehicleId == null) {
      this.gpsPings.set([]);
      return;
    }
    this.loading.set(true);
    this.mobilityMgmtService.getLiveRoute(vehicleId).subscribe({
      next: (res) => {
        this.gpsPings.set(res.success && res.data ? res.data : []);
        this.loading.set(false);
      },
      error: () => {
        this.gpsPings.set([]);
        this.loading.set(false);
        this.notify.error('Could not load live GPS data for the selected vehicle.');
      }
    });
  }

  loadAi() {
    this.mobilityMgmtService.getAiDashboard().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.aiPredictions.set(res.data.content || res.data || []);
        } else {
          this.aiPredictions.set([]);
        }
      },
      error: () => this.aiPredictions.set([])
    });
  }

  loadBackupHistory() {
    this.platformService.getBackups(0, 20).subscribe({
      next: (res) => {
        if (res.success && res.data?.content) {
          this.backupHistory.set(res.data.content);
        } else {
          this.backupHistory.set([]);
        }
      },
      error: () => this.backupHistory.set([])
    });
  }

  onVehicleChange(vhId: any) {
    this.selectedVehicleId.set(+vhId);
    this.loadGps();
  }

  triggerBackup() {
    this.loading.set(true);
    this.platformService.triggerBackup().subscribe({
      next: (res) => {
        if (res.success) {
          this.notify.success(res.message || 'Database backup started successfully.');
          this.loadBackupHistory();
        } else {
          this.notify.error(res.message || 'Backup could not be started.');
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err?.error?.message || 'Backup failed. Platform Admin access may be required.');
      }
    });
  }
}
