import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthService, UserRole } from './auth.service';

/* ──────────────────────────── Types ──────────────────────────── */

export interface UserResponse {
  userId: number;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  isActive: boolean;
  walletBalance: number;
}

/* ──────────────────────────── Service ─────────────────────────── */

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly API_BASE = '/api/admin';

  private http = inject(HttpClient);
  private auth = inject(AuthService);

  /* ── Build headers with X-User-Id from current session ── */

  private adminHeaders(): HttpHeaders {
    const session = this.auth.getSession();
    return new HttpHeaders({
      'X-User-Id': String(session?.userId ?? ''),
    });
  }

  /* ── GET /api/admin/users → all registered users ── */

  getAllUsers(): Observable<UserResponse[]> {
    const url = `${this.API_BASE}/users`;
    console.log('[AdminService] GET', url);

    return this.http
      .get<UserResponse[]>(url, { headers: this.adminHeaders() })
      .pipe(
        tap((res) =>
          console.log('[AdminService] getAllUsers success →', res.length, 'users')
        ),
        catchError((err) => AdminService.handleError(err, url))
      );
  }

  /* ── PUT /api/admin/users/{id}/toggle-status → flips isActive ── */

  toggleUserStatus(userId: number): Observable<UserResponse> {
    const url = `${this.API_BASE}/users/${userId}/toggle-status`;
    console.log('[AdminService] PUT', url);

    return this.http
      .put<UserResponse>(url, {}, { headers: this.adminHeaders() })
      .pipe(
        tap((res) =>
          console.log('[AdminService] toggleUserStatus success →', res)
        ),
        catchError((err) => AdminService.handleError(err, url))
      );
  }

  /* ── Error handler (mirrors AuthService.handleError) ── */

  private static handleError(
    err: HttpErrorResponse,
    url: string
  ): Observable<never> {
    console.error(`[AdminService] ERROR on ${url}`, {
      status: err.status,
      statusText: err.statusText,
      body: err.error,
    });

    let userMessage = 'Something went wrong. Please try again.';

    if (err.error) {
      if (typeof err.error === 'string') {
        userMessage = err.error;
      } else if (err.error.message) {
        userMessage = err.error.message;
      } else if (err.error.error) {
        // Spring Boot { "error": "..." } shape
        userMessage = err.error.error;
      } else if (typeof err.error === 'object') {
        const fieldErrors = Object.values(err.error).filter(
          (v) => typeof v === 'string'
        );
        if (fieldErrors.length > 0) {
          userMessage = fieldErrors.join('. ');
        }
      }
    }

    if (userMessage === 'Something went wrong. Please try again.') {
      if (err.status === 401) {
        userMessage = 'Admin access required. Please log in as an admin.';
      } else if (err.status === 404) {
        userMessage = 'User not found.';
      }
    }

    return throwError(() => ({ status: err.status, message: userMessage }));
  }
}
