import {
  Component,
  OnInit,
  signal,
  computed,
  ViewChild,
  ElementRef,
  AfterViewChecked,
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

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
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
      buildingId: [null],
    });

    console.log('[RegisterComponent] Form initialised');
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

  selectRole(role: UserRole): void {
    console.log('[RegisterComponent] selectRole ->', role);
    this.registerForm.patchValue({ role });
    this.registerForm.controls['role'].markAsTouched();
    this.serverError.set('');

    const vendorNameCtrl = this.f['vendorName'];
    const vendorDescCtrl = this.f['vendorDescription'];
    const buildingIdCtrl = this.f['buildingId'];

    if (role === 'VENDOR') {
      vendorNameCtrl.setValidators([Validators.required, Validators.minLength(2)]);
      vendorDescCtrl.setValidators([Validators.required, Validators.minLength(10)]);
      buildingIdCtrl.setValidators([Validators.required, Validators.min(1)]);
    } else {
      vendorNameCtrl.clearValidators();
      vendorNameCtrl.reset('');
      vendorDescCtrl.clearValidators();
      vendorDescCtrl.reset('');
      buildingIdCtrl.clearValidators();
      buildingIdCtrl.reset(null);
    }

    [vendorNameCtrl, vendorDescCtrl, buildingIdCtrl].forEach((ctrl) =>
      ctrl.updateValueAndValidity()
    );
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
    console.log('[RegisterComponent] Registration success ->', res);
    this.router.navigate(['/login'], {
      queryParams: { registered: 'true' },
    });
  }

  private onRegisterError(err: { status: number; message: string }): void {
    console.error('[RegisterComponent] Registration error ->', err);
    this.isSubmitting.set(false);
    this.serverError.set(err.message);
    this.shouldScrollToError = true;
    console.log('[RegisterComponent] serverError set to:', this.serverError());
    console.log('[RegisterComponent] isSubmitting set to:', this.isSubmitting());
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