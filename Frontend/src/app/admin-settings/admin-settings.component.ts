import {
  Component, OnInit, ChangeDetectorRef, inject, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginResponse } from '../services/auth.service';
import { UserService, UserProfile } from '../services/user.service';
import { ToastService } from '../services/toast.service';
import { ThemeService, Theme } from '../services/theme.service';

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-settings.component.html',
  styleUrls: ['./admin-settings.component.css']
})
export class AdminSettingsComponent implements OnInit {
  private auth     = inject(AuthService);
  private users    = inject(UserService);
  private toast    = inject(ToastService);
  private themeSvc = inject(ThemeService);
  private router   = inject(Router);
  private cdr      = inject(ChangeDetectorRef);

  user: LoginResponse | null = null;
  theme: Theme = 'light';

  isLoading = signal(true);
  hasError  = signal(false);
  profile = signal<UserProfile | null>(null);

  editName = '';
  editPhone = '';
  savingProfile = signal(false);

  togglingNotifs = signal(false);

  pwdCurrent = '';
  pwdNew = '';
  pwdConfirm = '';
  changingPassword = signal(false);
  pwdError = signal<string | null>(null);

  // Mobile sidebar
  mobileMenuOpen = false;

  ngOnInit(): void {
    const session = this.auth.getSession();
    if (!session?.userId || session.role !== 'ADMIN') {
      this.router.navigate(['/login']);
      return;
    }
    this.user = session;

    this.themeSvc.theme$.subscribe(t => { this.theme = t; this.cdr.detectChanges(); });
    this.loadProfile(session.userId);
  }

  toggleTheme(): void {
    this.themeSvc.toggle();
    this.toast.success(this.themeSvc.current === 'dark'
      ? 'Theme updated to Dark Mode'
      : 'Theme updated to Light Mode');
  }
  logout(): void {
    this.auth.clearSession();
    this.router.navigate(['/login']);
  }
  toggleMobileMenu(): void { this.mobileMenuOpen = !this.mobileMenuOpen; }
  closeMobileMenu(): void { this.mobileMenuOpen = false; }

  initials(name: string): string {
    return (name ?? '').split(' ').filter(Boolean).slice(0, 2)
      .map(p => p[0]).join('').toUpperCase();
  }

  private loadProfile(userId: number): void {
    this.isLoading.set(true);
    this.users.getProfile(userId).subscribe({
      next: (p) => {
        this.profile.set(p);
        this.editName = p.name;
        this.editPhone = p.phone;
        this.isLoading.set(false);
        this.cdr.detectChanges();
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
        this.toast.error('Could not load your profile.');
        this.cdr.detectChanges();
      }
    });
  }

  saveProfile(form: NgForm): void {
    const p = this.profile();
    if (!p || form.invalid) return;
    if (this.editName.trim() === p.name && this.editPhone.trim() === p.phone) {
      this.toast.info('Nothing changed in your profile.');
      return;
    }
    this.savingProfile.set(true);
    this.users.updateProfile(p.userId, {
      name: this.editName.trim(),
      phone: this.editPhone.trim()
    }).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        const session = this.auth.getSession();
        if (session) this.auth.storeSession({ ...session, name: updated.name });
        this.savingProfile.set(false);
        this.toast.success('Profile updated');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.savingProfile.set(false);
        this.toast.error(err?.error?.message || 'Could not update profile.');
        this.cdr.detectChanges();
      }
    });
  }

  toggleNotifications(): void {
    const p = this.profile();
    if (!p || this.togglingNotifs()) return;
    const next = !p.notificationsEnabled;
    this.togglingNotifs.set(true);
    this.users.setNotifications(p.userId, next).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.togglingNotifs.set(false);
        this.toast.success(next ? 'Notifications enabled' : 'Notifications muted');
        this.cdr.detectChanges();
      },
      error: () => {
        this.togglingNotifs.set(false);
        this.toast.error('Could not update notification setting.');
        this.cdr.detectChanges();
      }
    });
  }

  changePassword(form: NgForm): void {
    const p = this.profile();
    if (!p) return;
    this.pwdError.set(null);

    if (this.pwdNew.length < 6) {
      this.pwdError.set('New password must be at least 6 characters.'); return;
    }
    if (this.pwdNew !== this.pwdConfirm) {
      this.pwdError.set('New password and confirmation do not match.'); return;
    }
    if (this.pwdNew === this.pwdCurrent) {
      this.pwdError.set('New password must differ from current password.'); return;
    }

    this.changingPassword.set(true);
    this.users.changePassword(p.userId, {
      currentPassword: this.pwdCurrent,
      newPassword: this.pwdNew
    }).subscribe({
      next: () => {
        this.changingPassword.set(false);
        this.pwdCurrent = this.pwdNew = this.pwdConfirm = '';
        form.resetForm();
        this.toast.success('Password updated successfully');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.changingPassword.set(false);
        const msg = err?.error?.message || err?.error?.error || 'Could not change password.';
        this.pwdError.set(typeof msg === 'string' ? msg : 'Could not change password.');
        this.cdr.detectChanges();
      }
    });
  }
}
