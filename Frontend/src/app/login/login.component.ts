import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService, LoginPayload, UserRole } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  showPassword = false;

  isSubmitting = signal(false);
  serverError = signal('');
  showRegisteredBanner = signal(false);

  roles: UserRole[] = ['EMPLOYEE', 'VENDOR', 'ADMIN'];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      role: ['', [Validators.required]],
    });

    this.route.queryParams.subscribe((params) => {
      this.showRegisteredBanner.set(params['registered'] === 'true');
    });

    console.log('[LoginComponent] Form initialised');
  }

  get f() {
    return this.loginForm.controls;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  selectRole(role: UserRole): void {
    this.loginForm.patchValue({ role });
    this.loginForm.controls['role'].markAsTouched();
    this.serverError.set('');
  }

  onSubmit(): void {
    console.log('[LoginComponent] onSubmit() fired');
    console.log('[LoginComponent] Form status ->', this.loginForm.status);

    this.loginForm.markAllAsTouched();

    if (this.loginForm.invalid) {
      console.warn('[LoginComponent] Form INVALID');
      return;
    }

    console.log('[LoginComponent] Form VALID - sending request...');
    this.isSubmitting.set(true);
    this.serverError.set('');
    this.showRegisteredBanner.set(false);

    const payload: LoginPayload = this.loginForm.value;
    console.log('[LoginComponent] Payload ->', payload);

    this.authService.login(payload).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        console.log('[LoginComponent] Login success ->', response);

        // Store { userId, name, email, role } in localStorage
        this.authService.storeSession(response);

        // Route based on role returned by the backend
        switch (response.role) {
          case 'ADMIN':
            this.router.navigate(['/admin/dashboard']);
            break;
          case 'VENDOR':
            this.router.navigate(['/vendor/dashboard']);
            break;
          default:
            this.router.navigate(['/dashboard']);
        }
      },
      error: (err: { status: number; message: string }) => {
        console.error('[LoginComponent] Login error ->', err);
        this.isSubmitting.set(false);
        this.serverError.set(err.message);
        console.log('[LoginComponent] serverError set to:', this.serverError());
      },
    });
  }
}