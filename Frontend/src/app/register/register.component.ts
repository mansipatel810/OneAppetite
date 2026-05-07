import {
  Component,
  OnInit,
  signal,
  computed,
  ViewChild,
  ElementRef,
  AfterViewChecked,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  AuthService,
  RegisterPayload,
  VendorRegisterPayload,
  UserRole,
} from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { FoodService } from '../services/food.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
})
export class RegisterComponent implements OnInit, AfterViewChecked {
  registerForm!: FormGroup;
  showPassword = false;

  /**
   * Using Angular signals for these two properties.
   * Signals notify the template directly when their value changes,
   * bypassing Zone.js and change detection entirely.
   * This fixes the UI-not-updating issue in Angular 17+ SSR/hydrated apps.
   */
  isSubmitting = signal(false);
  serverError = signal('');

  private shouldScrollToError = false;

  @ViewChild('errorAlert') errorAlert?: ElementRef<HTMLDivElement>;

  roles: UserRole[] = ['EMPLOYEE', 'VENDOR', 'ADMIN'];

  get isVendor(): boolean {
    return this.registerForm?.get('role')?.value === 'VENDOR';
  }

  private toast = inject(ToastService);
  private foodSvc = inject(FoodService);

  /* Cascade dropdown state — populated when the Vendor role is selected. */
  cities:    any[] = [];
  campuses:  any[] = [];
  buildings: any[] = [];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      role: ['', [Validators.required]],
      vendorName: [''],
      vendorDescription: [''],
      // Vendor-only location fields. We store STRINGS so they line up with
      // <option [value]="..."> in the template — that's what fixed the
      // "Chennai can't be selected" bug. Validators are wired in selectRole().
      cityId:     [''],
      campusId:   [''],
      buildingId: [''],
      floor:      [''],
      wing:       [''],
    });

    console.log('[RegisterComponent] Form initialised');
  }

  /* ── Cascade handlers ─────────────────────────────────── */
  onCityChange(event: Event): void {
    const raw = (event.target as HTMLSelectElement).value;
    const id  = raw ? Number(raw) : NaN;

    // Reset the downstream legs of the cascade.
    this.registerForm.patchValue({ cityId: raw, campusId: '', buildingId: '' });
    this.campuses  = [];
    this.buildings = [];

    if (Number.isFinite(id) && id > 0) {
      this.foodSvc.getCampuses(id).subscribe({
        next: (list) => this.campuses = list ?? [],
        error: () => this.toast.error('Could not load campuses for this city.'),
      });
    }
  }

  onCampusChange(event: Event): void {
    const raw = (event.target as HTMLSelectElement).value;
    const id  = raw ? Number(raw) : NaN;

    this.registerForm.patchValue({ campusId: raw, buildingId: '' });
    this.buildings = [];

    if (Number.isFinite(id) && id > 0) {
      this.foodSvc.getBuildings(id).subscribe({
        next: (list) => this.buildings = list ?? [],
        error: () => this.toast.error('Could not load buildings for this campus.'),
      });
    }
  }

  onBuildingChange(event: Event): void {
    const raw = (event.target as HTMLSelectElement).value;
    this.registerForm.patchValue({ buildingId: raw });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToError && this.errorAlert) {
      this.errorAlert.nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
      this.shouldScrollToError = false;
    }
  }

  get f() {
    return this.registerForm.controls;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  comingSoon(provider: string): void {
    this.toast.info(`${provider} sign-up is coming soon — please use the form below for now.`);
  }

  selectRole(role: UserRole): void {
    console.log('[RegisterComponent] selectRole ->', role);
    this.registerForm.patchValue({ role });
    this.registerForm.controls['role'].markAsTouched();
    this.serverError.set('');

    const vendorOnly = ['vendorName', 'vendorDescription', 'cityId', 'campusId', 'buildingId', 'floor', 'wing'];

    if (role === 'VENDOR') {
      this.f['vendorName'].setValidators([Validators.required, Validators.minLength(2)]);
      this.f['vendorDescription'].setValidators([Validators.required, Validators.minLength(10)]);
      // City/campus/building are stored as STRINGS — Validators.required is
      // enough since '' fails it. Validators.min would be a no-op on strings.
      this.f['cityId'].setValidators([Validators.required]);
      this.f['campusId'].setValidators([Validators.required]);
      this.f['buildingId'].setValidators([Validators.required]);
      this.f['floor'].setValidators([Validators.required, Validators.maxLength(16)]);
      this.f['wing'].setValidators([Validators.required, Validators.maxLength(16)]);

      // Lazy-load cities the first time a vendor signup is started.
      if (this.cities.length === 0) {
        this.foodSvc.getCities().subscribe(list => this.cities = list);
      }
    } else {
      // All vendor-only controls (string-typed) reset to the empty placeholder
      // value so the corresponding <option value=""> renders selected.
      vendorOnly.forEach(name => {
        const ctrl = this.f[name];
        ctrl.clearValidators();
        ctrl.reset('');
      });
      this.campuses = [];
      this.buildings = [];
    }

    vendorOnly.forEach(name => this.f[name].updateValueAndValidity());
  }

  onSubmit(): void {
    console.log('[RegisterComponent] onSubmit() fired');
    console.log('[RegisterComponent] Form status ->', this.registerForm.status);

    this.registerForm.markAllAsTouched();

    if (this.registerForm.invalid) {
      console.warn('[RegisterComponent] INVALID', this.getFormErrors());
      return;
    }

    console.log('[RegisterComponent] VALID - sending request...');
    this.isSubmitting.set(true);
    this.serverError.set('');

    const formVal = this.registerForm.value;

    if (formVal.role === 'VENDOR') {
      const vendorPayload: VendorRegisterPayload = {
        name: formVal.fullName,
        email: formVal.email,
        phone: formVal.phoneNumber,
        password: formVal.password,
        vendorName: formVal.vendorName,
        vendorDescription: formVal.vendorDescription,
        buildingId: Number(formVal.buildingId),
        floor: String(formVal.floor || '').trim(),
        wing:  String(formVal.wing  || '').trim(),
      };

      console.log('[RegisterComponent] Vendor payload ->', vendorPayload);

      this.authService.registerVendor(vendorPayload).subscribe({
        next: (res) => this.onRegisterSuccess(res),
        error: (err: { status: number; message: string }) =>
          this.onRegisterError(err),
      });
    } else {
      const payload: RegisterPayload = {
        name: formVal.fullName,
        email: formVal.email,
        phone: formVal.phoneNumber,
        password: formVal.password,
        role: formVal.role,
      };

      console.log('[RegisterComponent] Standard payload ->', payload);

      this.authService.register(payload).subscribe({
        next: (res) => this.onRegisterSuccess(res),
        error: (err: { status: number; message: string }) =>
          this.onRegisterError(err),
      });
    }
  }

  private onRegisterSuccess(res: any): void {
    this.isSubmitting.set(false);
    this.toast.success('Account created — please sign in.');
    this.router.navigate(['/login'], {
      queryParams: { registered: 'true' },
    });
  }

  private onRegisterError(err: { status: number; message: string }): void {
    this.isSubmitting.set(false);
    this.serverError.set(err.message);
    this.shouldScrollToError = true;
    this.toast.error(err.message || 'Registration failed.');
  }

  private getFormErrors(): Record<string, any> {
    const errors: Record<string, any> = {};
    Object.keys(this.registerForm.controls).forEach((key) => {
      const ctrl = this.registerForm.controls[key];
      if (ctrl.errors) {
        errors[key] = ctrl.errors;
      }
    });
    return errors;
  }
}