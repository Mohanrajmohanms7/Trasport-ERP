import {
  DestroyRef,
  Directive,
  Injector,
  OnInit,
  computed,
  inject,
  input,
  signal
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NgControl,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FfSize } from '../../infrastructure/enums/ff-size.enum';
import { FF_VALIDATION_MESSAGES } from '../../infrastructure/constants/validation-messages';
import { ffResolveErrorMessage } from '../../infrastructure/utils/ff.utils';
import { FfPermissionService } from '../../infrastructure/services/ff-permission.service';
import { FfBusinessRuleService } from '../../infrastructure/services/ff-permission.service';

/**
 * Abstract base for all ff-* form controls.
 * Implements ControlValueAccessor + Validator with signals,
 * permission gating, and business-rule effects.
 *
 * Subclasses must:
 * - Provide NG_VALUE_ACCESSOR / NG_VALIDATORS
 * - Call super.ngOnInit()
 * - Use emitValue() / markTouched() / onBlur() / onFocus()
 */
@Directive()
export abstract class FfControlBase<T = unknown> implements ControlValueAccessor, Validator, OnInit {
  protected readonly destroyRef = inject(DestroyRef);
  protected readonly injector = inject(Injector);
  protected readonly permissionService = inject(FfPermissionService);
  protected readonly ruleService = inject(FfBusinessRuleService);

  readonly label = input<string>('');
  readonly hint = input<string>('');
  readonly placeholder = input<string>('');
  readonly required = input<boolean>(false);
  readonly readonly = input<boolean>(false);
  readonly disabledInput = input(false, { alias: 'disabled' });
  readonly loading = input<boolean>(false);
  readonly skeleton = input<boolean>(false);
  readonly prefixIcon = input<string>('');
  readonly suffixIcon = input<string>('');
  readonly tooltip = input<string>('');
  readonly size = input<FfSize>('md');
  readonly permission = input<string>('');
  readonly ruleId = input<string>('');
  readonly errorMessages = input<Record<string, string>>({});
  readonly controlId = input<string>('');

  readonly value = signal<T | null>(null);
  readonly touched = signal(false);
  readonly focused = signal(false);
  readonly cvaDisabled = signal(false);
  readonly controlErrors = signal<ValidationErrors | null>(null);
  /** True when parent FormControl has Validators.required (shows * without extra [required] input). */
  private readonly formRequired = signal(false);

  protected onChange: (value: T | null) => void = () => undefined;
  protected onTouched: () => void = () => undefined;
  protected onValidatorChange: () => void = () => undefined;

  private ngControl: NgControl | null = null;
  private readonly generatedId = `ff-ctrl-${Math.random().toString(36).slice(2, 9)}`;

  readonly inputId = computed(() => this.controlId() || this.generatedId);

  readonly isDisabled = computed(
    () =>
      this.cvaDisabled() ||
      this.disabledInput() ||
      this.ruleEffects().disabled === true ||
      !this.permissionAllowed()
  );

  readonly isReadonly = computed(
    () => this.readonly() || this.ruleEffects().readonly === true
  );

  readonly isRequired = computed(
    () => this.required() || this.formRequired() || this.ruleEffects().required === true
  );

  readonly isVisible = computed(() => {
    const effects = this.ruleEffects();
    if (effects.hidden === true || effects.visible === false) return false;
    return this.permissionAllowed();
  });

  readonly displayLabel = computed(
    () => this.ruleEffects().label || this.label()
  );

  readonly displayTooltip = computed(
    () => this.ruleEffects().tooltip || this.tooltip()
  );

  readonly hasError = computed(
    () => this.touched() && !!this.controlErrors()
  );

  readonly errorMessage = computed(() =>
    ffResolveErrorMessage(
      this.controlErrors(),
      this.errorMessages(),
      FF_VALIDATION_MESSAGES
    )
  );

  readonly ruleEffects = computed(() => this.ruleService.evaluate(this.ruleId() || undefined));

  readonly permissionAllowed = computed(() =>
    this.permissionService.can(this.permission() || undefined)
  );

  readonly sizeClass = computed(() => {
    switch (this.size()) {
      case 'sm':
        return 'ff-control--sm';
      case 'lg':
        return 'ff-control--lg';
      default:
        return 'ff-control--md';
    }
  });

  ngOnInit(): void {
    this.ngControl = this.injector.get(NgControl, null, { optional: true, self: true });

    queueMicrotask(() => {
      const control = this.ngControl?.control;
      if (!control) return;

      // Mirror FormControl required validator so labels show *
      try {
        if (control.hasValidator?.(Validators.required)) {
          this.formRequired.set(true);
          this.onValidatorChange();
        }
      } catch {
        /* older Angular / custom controls */
      }

      const syncFromControl = () => {
        this.controlErrors.set(control.errors);
        if (control.touched) this.touched.set(true);
      };

      control.statusChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(syncFromControl);
      control.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(syncFromControl);
      syncFromControl();
    });
  }

  writeValue(value: T | null): void {
    this.value.set(value);
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.cvaDisabled.set(isDisabled);
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (!this.isRequired()) return null;
    // Prefer FormControl value — avoids false "required" when CVA signal is briefly empty
    const raw = control.value ?? this.value();
    const empty =
      raw === null ||
      raw === undefined ||
      (typeof raw === 'string' && raw.trim() === '') ||
      (Array.isArray(raw) && raw.length === 0);
    return empty ? { required: true } : null;
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  protected emitValue(value: T | null): void {
    this.value.set(value);
    this.onChange(value);
  }

  protected markTouched(): void {
    this.touched.set(true);
    this.onTouched();
    const control = this.ngControl?.control;
    if (control) {
      this.controlErrors.set(control.errors);
    }
  }

  onFocus(): void {
    this.focused.set(true);
  }

  onBlur(): void {
    this.focused.set(false);
    this.markTouched();
  }
}
