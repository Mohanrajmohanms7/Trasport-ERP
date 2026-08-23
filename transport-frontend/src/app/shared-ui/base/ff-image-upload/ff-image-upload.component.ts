import { ChangeDetectionStrategy, Component, forwardRef, input, OnInit, signal } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-image-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-image-upload.component.html',
  styleUrl: './ff-image-upload.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfImageUploadComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfImageUploadComponent), multi: true }
  ]
})
export class FfImageUploadComponent extends FfControlBase<File | null> implements OnInit {
  readonly accept = input<string>('image/*');
  readonly maxSizeMb = input<number>(5);
  readonly previewHeight = input<string>('160px');

  readonly previewUrl = signal<string | null>(null);
  readonly error = signal('');

  override writeValue(value: File | null): void {
    super.writeValue(value);
    this.updatePreview(value);
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.updatePreview(this.value());
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    (event.target as HTMLInputElement).value = '';
    this.setFile(file ?? null);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    if (this.isDisabled() || this.isReadonly()) return;
    const file = event.dataTransfer?.files?.[0];
    if (file?.type.startsWith('image/')) {
      this.setFile(file);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  remove(): void {
    this.revokePreview();
    this.emitValue(null);
    this.previewUrl.set(null);
    this.error.set('');
  }

  private setFile(file: File | null): void {
    this.error.set('');
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.error.set('Please select an image file');
      return;
    }
    const maxBytes = this.maxSizeMb() * 1024 * 1024;
    if (file.size > maxBytes) {
      this.error.set(`Image exceeds ${this.maxSizeMb()} MB limit`);
      return;
    }
    this.emitValue(file);
    this.updatePreview(file);
    this.markTouched();
  }

  private updatePreview(file: File | null): void {
    this.revokePreview();
    if (!file) {
      this.previewUrl.set(null);
      return;
    }
    this.previewUrl.set(URL.createObjectURL(file));
  }

  private revokePreview(): void {
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);
  }
}
