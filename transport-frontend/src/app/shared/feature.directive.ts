import { Directive, Input, TemplateRef, ViewContainerRef, effect, inject, signal } from '@angular/core';
import { FeatureService } from '../services/feature.service';

/** Shows its content only when the client's plan includes the feature:  <button *ffFeature="'bulk-upload'">…</button> */
@Directive({ selector: '[ffFeature]', standalone: true })
export class FeatureDirective {
  private tpl = inject(TemplateRef<unknown>);
  private vcr = inject(ViewContainerRef);
  private features = inject(FeatureService);
  private code = signal<string>('');
  private shown = false;

  @Input() set ffFeature(code: string) { this.code.set(code); }

  constructor() {
    effect(() => {
      const ok = !this.code() || this.features.has(this.code());
      if (ok && !this.shown) { this.vcr.createEmbeddedView(this.tpl); this.shown = true; }
      if (!ok && this.shown) { this.vcr.clear(); this.shown = false; }
    });
  }
}
