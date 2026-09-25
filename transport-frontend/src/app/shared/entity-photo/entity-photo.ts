import { Component, OnChanges, OnDestroy, inject, input, output, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

/**
 * Photo with upload / replace / remove: <app-entity-photo type="drivers" [entityId]="d.id" [photoFile]="d.photoFile" />.
 * Images are fetched with the login token, so they respect company access rules.
 */
@Component({
  selector: 'app-entity-photo',
  standalone: true,
  template: `
    <div class="flex items-center gap-3">
      <div class="shrink-0 overflow-hidden border border-[var(--ff-border-default)] bg-[var(--ff-surface-hover)] flex items-center justify-center"
           [style.width.px]="size()" [style.height.px]="size()" [style.border-radius]="round() ? '9999px' : '12px'">
        @if (src()) {
          <img [src]="src()" [alt]="alt()" class="w-full h-full object-cover" />
        } @else {
          <span class="material-icons text-[var(--ff-text-muted)]" [style.font-size.px]="size() / 2">{{ icon() }}</span>
        }
      </div>
      @if (!readonly() && entityId()) {
        <div class="flex flex-col gap-1">
          <label class="text-xs font-semibold text-[var(--ff-color-primary-600)] cursor-pointer">
            {{ busy() ? 'Uploading…' : (src() ? 'Change photo' : 'Add photo') }}
            <input type="file" class="hidden" accept="image/jpeg,image/png,image/webp" (change)="upload($event)" [disabled]="busy()" />
          </label>
          @if (src()) {
            <button type="button" class="text-xs text-left text-[var(--ff-text-muted)] hover:text-[var(--ff-color-danger-500)]" (click)="remove()">Remove</button>
          }
          <span class="text-[10px] text-[var(--ff-text-muted)]">JPG, PNG or WEBP, up to 5 MB</span>
        </div>
      }
    </div>
  `
})
export class EntityPhotoComponent implements OnChanges, OnDestroy {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);

  readonly type = input.required<'vehicles' | 'drivers' | 'spare-parts'>();
  readonly entityId = input<number | null | undefined>(null);
  readonly photoFile = input<string | null | undefined>(null);
  readonly size = input<number>(72);
  readonly round = input<boolean>(false);
  readonly icon = input<string>('photo_camera');
  readonly alt = input<string>('Photo');
  readonly readonly = input<boolean>(false);
  readonly changed = output<string | null>();

  src = signal<string | null>(null);
  busy = signal(false);
  private current: string | null = null;

  ngOnChanges(): void {
    this.show(this.photoFile() ?? null);
  }

  ngOnDestroy(): void {
    if (this.src()) URL.revokeObjectURL(this.src()!);
  }

  private show(file: string | null): void {
    if (file === this.current) return;
    this.current = file;
    if (this.src()) URL.revokeObjectURL(this.src()!);
    this.src.set(null);
    if (!file) return;
    this.http.get(`/api/v1/files/download/${file}`, { responseType: 'blob' }).subscribe({
      next: b => this.src.set(URL.createObjectURL(b)),
      error: () => this.src.set(null)
    });
  }

  upload(ev: Event): void {
    const el = ev.target as HTMLInputElement;
    const file = el.files?.[0];
    el.value = '';
    if (!file || !this.entityId()) return;
    const form = new FormData();
    form.append('file', file);
    this.busy.set(true);
    this.http.post<any>(`/api/v1/photos/${this.type()}/${this.entityId()}`, form).subscribe({
      next: r => {
        this.busy.set(false);
        const name = r?.data?.photoFile ?? null;
        this.show(name);
        this.changed.emit(name);
        this.notify.success('Photo saved');
      },
      error: e => {
        this.busy.set(false);
        this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Photo upload failed');
      }
    });
  }

  remove(): void {
    if (!this.entityId()) return;
    this.http.delete<any>(`/api/v1/photos/${this.type()}/${this.entityId()}`).subscribe({
      next: () => { this.show(null); this.changed.emit(null); this.notify.success('Photo removed'); },
      error: () => this.notify.error('Could not remove photo')
    });
  }
}
