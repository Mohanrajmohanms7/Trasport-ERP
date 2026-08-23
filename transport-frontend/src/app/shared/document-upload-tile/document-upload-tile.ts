import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-document-upload-tile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './document-upload-tile.html',
  styles: []
})
export class DocumentUploadTileComponent {
  @Input() label: string = '';
  @Input() icon: string = 'file_present';
  @Input() colorClass: 'blue' | 'green' | 'amber' | 'rose' | 'purple' = 'blue';
  @Input() fileState: string = '';

  @Output() onUpload = new EventEmitter<void>();

  get bgIconClass(): string {
    const maps = {
      blue: 'bg-blue-50 text-blue-600',
      green: 'bg-emerald-50 text-emerald-600',
      amber: 'bg-amber-50 text-amber-600',
      rose: 'bg-rose-50 text-rose-600',
      purple: 'bg-purple-50 text-purple-600'
    };
    return maps[this.colorClass] || maps.blue;
  }

  triggerUpload() {
    this.onUpload.emit();
  }
}
