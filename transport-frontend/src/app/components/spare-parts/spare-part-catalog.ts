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

  editingId = signal<number | null>(null);
  showForm = signal(false);
  search = signal('');
  reorderLevel = signal('');
  status = signal('ACTIVE');

  filtered(): SparePart[] {
    const q = this.search().trim().toLowerCase();
    return q ? this.rows().filter(r => (r.code + ' ' + r.name).toLowerCase().includes(q)) : this.rows();
  }

  openNew() {
    this.editingId.set(null);
    this.code.set(''); this.name.set(''); this.description.set('');
    this.defaultUomId.set(''); this.defaultRate.set('0'); this.reorderLevel.set(''); this.status.set('ACTIVE');
    this.showForm.set(true);
  }

  openEdit(row: SparePart) {
    this.editingId.set(row.id);
    this.code.set(row.code); this.name.set(row.name); this.description.set(row.description || '');
    this.defaultUomId.set(row.defaultUomId ? String(row.defaultUomId) : '');
    this.defaultRate.set(row.defaultRate != null ? String(row.defaultRate) : '0');
    this.reorderLevel.set(row.reorderLevel != null ? String(row.reorderLevel) : '');
    this.status.set(row.status || 'ACTIVE');
    this.showForm.set(true);
  }

  save() {
    if (this.editingId()) {
      this.error.set(null);
      this.feedback.set(null);
      if (!this.name().trim()) { this.error.set('Name is required.'); return; }
      this.spareParts.update(this.editingId()!, {
        name: this.name().trim(),
        description: this.description().trim() || null,
        defaultUomId: this.defaultUomId() ? Number(this.defaultUomId()) : null,
        defaultRate: this.defaultRate().trim() ? Number(this.defaultRate()) : 0,
        reorderLevel: this.reorderLevel().trim() ? Number(this.reorderLevel()) : null,
        status: this.status()
      }).subscribe({
        next: res => {
          if (res?.success) { this.feedback.set('Spare part updated.'); this.showForm.set(false); this.load(); }
          else this.error.set(res?.message || 'Unable to update the spare part.');
        },
        error: err => this.error.set(workOrderError(err))
      });
      return;
    }
    this.create();
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
      defaultRate: this.defaultRate().trim() ? Number(this.defaultRate()) : 0,
      reorderLevel: this.reorderLevel().trim() ? Number(this.reorderLevel()) : null,
      status: this.status()
    }).subscribe({
      next: res => {
        if (res?.success) {
          this.feedback.set('Spare part created.');
          this.showForm.set(false);
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
