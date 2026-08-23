import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FfControlBase } from '../_base/ff-control.base';

@Component({
  selector: 'ff-file-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-file-upload.component.html',
  styleUrl: './ff-file-upload.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FfFileUploadComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FfFileUploadComponent), multi: true }
  ]
})
export class FfFileUploadComponent extends FfControlBase<File | File[] | null> {
  readonly accept = input<string>('');
  readonly multiple = input<boolean>(false);
  readonly maxSizeMb = input<number>(10);

  readonly dragOver = signal(false);
  readonly error = signal('');

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.handleFiles(input.files);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    if (this.isDisabled() || this.isReadonly()) return;
    this.handleFiles(event.dataTransfer?.files ?? null);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.isDisabled()) this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  removeFile(index?: number): void {
    if (this.multiple() && Array.isArray(this.value())) {
      const files = [...(this.value() as File[])];
      if (index !== undefined) files.splice(index, 1);
      this.emitValue(files.length ? files : null);
    } else {
      this.emitValue(null);
    }
    this.error.set('');
  }

  fileLabel(file: File): string {
    return `${file.name} (${this.formatSize(file.size)})`;
  }

  filesList(): File[] {
    const v = this.value();
    if (!v) return [];
    return Array.isArray(v) ? v : [v];
  }

  private handleFiles(fileList: FileList | null): void {
    if (!fileList?.length) return;
    this.error.set('');
    const maxBytes = this.maxSizeMb() * 1024 * 1024;
    const files = Array.from(fileList).filter(f => {
      if (f.size > maxBytes) {
        this.error.set(`File "${f.name}" exceeds ${this.maxSizeMb()} MB limit`);
        return false;
      }
      return true;
    });
    if (!files.length) return;

    if (this.multiple()) {
      const existing = this.filesList();
      this.emitValue([...existing, ...files]);
    } else {
      this.emitValue(files[0]);
    }
    this.markTouched();
  }

  private formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
