import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ff-expansion-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-expansion-panel.component.html',
  styleUrl: './ff-expansion-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FfExpansionPanelComponent implements OnInit {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
  readonly expanded = input<boolean>(false);
  readonly disabled = input<boolean>(false);

  readonly expandedChange = output<boolean>();

  readonly isOpen = signal(false);

  ngOnInit(): void {
    this.isOpen.set(this.expanded());
  }

  toggle(): void {
    if (this.disabled()) return;
    this.isOpen.update(v => !v);
    this.expandedChange.emit(this.isOpen());
  }
}
