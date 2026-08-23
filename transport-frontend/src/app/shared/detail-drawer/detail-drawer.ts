import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-detail-drawer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './detail-drawer.html',
  styles: []
})
export class DetailDrawerComponent {
  @Input() isOpen: boolean = false;
  @Input() title: string = '';
  @Input() subTitle: string = '';
  @Input() tabs: string[] = [];
  @Input() activeTab: string = '';

  @Output() onClose = new EventEmitter<void>();
  @Output() onTabChange = new EventEmitter<string>();

  closeDrawer() {
    this.onClose.emit();
  }

  selectTab(tab: string) {
    this.activeTab = tab;
    this.onTabChange.emit(tab);
  }
}
