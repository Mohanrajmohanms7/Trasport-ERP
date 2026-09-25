import { EntityPhotoComponent } from '../../shared/entity-photo/entity-photo';
import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { SparePart, SparePartService, SpareUom } from '../../services/spare-part.service';
import { workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-spare-part-catalog',
  standalone: true,
  imports: [EntityPhotoComponent, ExportButtonsComponent, CommonModule, FormsModule],
  templateUrl: './spare-part-catalog.html'
})
export class SparePartCatalogComponent implements OnInit {
  /** Photo edit is available inline; the server checks company access. */
  readonly canWritePhoto = true;
  private spareParts = inject(SparePartService);
  private auth = inject(AuthService);

  loading = signal(false);
  error = signal<string | null>(null);
  feedback = signal<string | null>(null);
  rows = signal<SparePart[]>([]);
  uoms = signal<SpareUom[]>([]);
  canWrite = signal(false);

  code = signal('');
  name = signal('');
  description = signal('');
  defaultUomId = signal('');
  defaultRate = signal('0');

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    this.canWrite.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER'));
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.spareParts.list().subscribe({
      next: res => {
        this.rows.set(res?.success && res.data ? res.data.content || [] : []);
        if (!res?.success) this.error.set(res?.message || 'Unable to load spare parts.');
        this.loading.set(false);
      },
      error: err => {
        this.error.set(workOrderError(err));
        this.loading.set(false);
      }
    });
    this.spareParts.uoms().subscribe({
      next: res => this.uoms.set(res?.success && res.data ? res.data : []),
      error: () => this.uoms.set([])
    });
  }

  create() {
    this.error.set(null);
    this.feedback.set(null);
    if (!this.code().trim() || !this.name().trim() || !this.defaultUomId()) {
      this.error.set('Code, name, and unit are required.');
      return;
    }
    this.spareParts.create({
      code: this.code().trim(),
      name: this.name().trim(),
      description: this.description().trim() || null,
      defaultUomId: Number(this.defaultUomId()),
      defaultRate: this.defaultRate().trim() ? Number(this.defaultRate()) : 0
    }).subscribe({
      next: res => {
        if (res?.success) {
          this.feedback.set('Spare part created.');
          this.code.set('');
          this.name.set('');
          this.description.set('');
          this.defaultRate.set('0');
          this.load();
        } else {
          this.error.set(res?.message || 'Unable to create the spare part.');
        }
      },
      error: err => this.error.set(workOrderError(err))
    });
  }
}
