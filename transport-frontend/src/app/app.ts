import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FfThemeService } from './shared-ui/infrastructure/services/ff-theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private theme = inject(FfThemeService);
  protected readonly title = signal('transport-frontend');

  ngOnInit(): void {
    this.theme.loadPersisted();
  }
}
