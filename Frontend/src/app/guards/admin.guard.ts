import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Functional route guard. Allows navigation only when a logged-in user
 * with role === 'ADMIN' is present in the current session.
 *
 * On failure: redirects to /login.
 */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const session = auth.getSession();

  if (!session) {
    console.warn('[adminGuard] no session — redirecting to /login');
    router.navigate(['/login']);
    return false;
  }

  if (session.role !== 'ADMIN') {
    console.warn(
      `[adminGuard] role=${session.role} is not ADMIN — redirecting to /login`
    );
    router.navigate(['/login']);
    return false;
  }

  return true;
};