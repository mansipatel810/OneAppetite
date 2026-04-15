import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

/* ──────────────────────────── Types ──────────────────────────── */

export type UserRole = 'EMPLOYEE' | 'VENDOR' | 'ADMIN';

export interface RegisterPayload {
  name: string;
  email: string;
  phone: string;
  password: string;
  role: UserRole;
}

export interface VendorRegisterPayload {
  name: string;
  email: string;
  phone: string;
  password: string;
  vendorName: string;
  vendorDescription: string;
  buildingId: number;
}

export interface LoginPayload {
  email: string;
  password: string;
  role: UserRole;
}

export interface LoginResponse {
  userId: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface RegisterResponse {
  [key: string]: any;
}

/* ──────────────────────────── Service ─────────────────────────── */

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly API_BASE = '/api/auth';

  constructor(private http: HttpClient) {}

  /* ── Register (Employee / Admin) ── */

  register(payload: RegisterPayload): Observable<RegisterResponse> {
    const url = `${this.API_BASE}/register`;
    console.log('[AuthService] POST', url, payload);

    return this.http.post<RegisterResponse>(url, payload).pipe(
      tap((res) => console.log('[AuthService] register success →', res)),
      catchError((err) => AuthService.handleError(err, url))
    );
  }

  /* ── Register (Vendor) ── */

  registerVendor(payload: VendorRegisterPayload): Observable<RegisterResponse> {
    const url = `${this.API_BASE}/register/vendor`;
    console.log('[AuthService] POST', url, payload);

    return this.http.post<RegisterResponse>(url, payload).pipe(
      tap((res) => console.log('[AuthService] registerVendor success →', res)),
      catchError((err) => AuthService.handleError(err, url))
    );
  }

  /* ── Login ── */

  login(credentials: LoginPayload): Observable<LoginResponse> {
    const url = `${this.API_BASE}/login`;
    console.log('[AuthService] POST', url, credentials);

    return this.http.post<LoginResponse>(url, credentials).pipe(
      tap((res) => console.log('[AuthService] login success →', res)),
      catchError((err) => AuthService.handleError(err, url))
    );
  }

  /* ── Session helpers ── */

  storeSession(data: LoginResponse): void {
    localStorage.setItem('oa_user', JSON.stringify(data));
  }

  getSession(): LoginResponse | null {
    const raw = localStorage.getItem('oa_user');
    return raw ? (JSON.parse(raw) as LoginResponse) : null;
  }

  clearSession(): void {
    localStorage.removeItem('oa_user');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('oa_user');
  }

  /* ── Error handler ── */

  private static handleError(
    err: HttpErrorResponse,
    url: string
  ): Observable<never> {
    console.error(`[AuthService] ERROR on ${url}`, {
      status: err.status,
      statusText: err.statusText,
      body: err.error,
    });

    let userMessage = 'Something went wrong. Please try again.';

    if (err.error) {
      if (typeof err.error === 'string') {
        // Plain string error body
        userMessage = err.error;
      } else if (err.error.message) {
        // { message: "Email already registered: x@y.com" }
        userMessage = err.error.message;
      } else if (err.error.error) {
        // { error: "Email domain not allowed. Use a corporate email." }
        // — Spring Boot sometimes uses "error" as the key
        userMessage = err.error.error;
      } else if (typeof err.error === 'object') {
        // { name: "Name is required", phone: "Phone is required" }
        // — @Valid field-level errors returned as a flat map
        const fieldErrors = Object.values(err.error).filter(
          (v) => typeof v === 'string'
        );
        if (fieldErrors.length > 0) {
          userMessage = fieldErrors.join('. ');
        }
      }
    }

    // Provide clear fallback messages for common status codes
    if (userMessage === 'Something went wrong. Please try again.') {
      if (err.status === 409) {
        userMessage = 'Email already registered';
      } else if (err.status === 401) {
        userMessage = 'Invalid email or password';
      }
    }

    return throwError(() => ({ status: err.status, message: userMessage }));
  }
}