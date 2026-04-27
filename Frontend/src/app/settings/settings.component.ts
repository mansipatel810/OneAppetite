import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserService, UserProfile } from '../services/user.service';
import { WalletService } from '../services/wallet.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {
  private auth   = inject(AuthService);
  private users  = inject(UserService);
  private wallet = inject(WalletService);
  private toast  = inject(ToastService);
  private router = inject(Router);
  private cdr    = inject(ChangeDetectorRef);

  isLoading = true;
  hasError  = false;
  profile: UserProfile | null = null;

  // Profile edit
  editName  = '';
  editPhone = '';
  savingProfile = false;

  // Notifications
  togglingNotifs = false;

  // Password
  pwdCurrent = '';
  pwdNew     = '';
  pwdConfirm = '';
  changingPassword = false;
  pwdError: string | null = null;

  // Wallet
  topUpAmount: number | null = null;
  topUpUpi = '';
  toppingUp = false;

  readonly quickAmounts = [100, 250, 500, 1000];

  ngOnInit(): void {
    const session = this.auth.getSession();
    if (!session?.userId) {
      this.router.navigate(['/login']);
      return;
    }
    this.load(session.userId);
  }

  private load(userId: number): void {
    this.isLoading = true;
    this.users.getProfile(userId).subscribe({
      next: (p) => {
        this.profile   = p;
        this.editName  = p.name;
        this.editPhone = p.phone;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.hasError  = true;
        this.isLoading = false;
        this.toast.error('Could not load your profile.');
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Profile ─────────────────────────────────────────────── */
  saveProfile(form: NgForm): void {
    if (!this.profile || form.invalid) return;
    if (this.editName.trim() === this.profile.name && this.editPhone.trim() === this.profile.phone) {
      this.toast.info('Nothing changed in your profile.');
      return;
    }

    this.savingProfile = true;
    this.users.updateProfile(this.profile.userId, {
      name:  this.editName.trim(),
      phone: this.editPhone.trim()
    }).subscribe({
      next: (p) => {
        this.profile      = p;
        this.savingProfile = false;
        this.toast.success('Profile updated');

        // Sync session display name so navbar avatar updates immediately
        const session = this.auth.getSession();
        if (session) {
          this.auth.storeSession({ ...session, name: p.name });
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.savingProfile = false;
        this.toast.error(err?.error?.message || 'Could not update profile.');
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Notifications ────────────────────────────────────────── */
  toggleNotifications(): void {
    if (!this.profile || this.togglingNotifs) return;
    const next = !this.profile.notificationsEnabled;
    this.togglingNotifs = true;

    this.users.setNotifications(this.profile.userId, next).subscribe({
      next: (p) => {
        this.profile = p;
        this.togglingNotifs = false;
        this.toast.success(next ? 'Notifications enabled' : 'Notifications muted');
        this.cdr.detectChanges();
      },
      error: () => {
        this.togglingNotifs = false;
        this.toast.error('Could not update notification setting.');
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Password ─────────────────────────────────────────────── */
  changePassword(form: NgForm): void {
    if (!this.profile) return;
    this.pwdError = null;

    if (this.pwdNew.length < 6) {
      this.pwdError = 'New password must be at least 6 characters.';
      return;
    }
    if (this.pwdNew !== this.pwdConfirm) {
      this.pwdError = 'New password and confirmation do not match.';
      return;
    }
    if (this.pwdNew === this.pwdCurrent) {
      this.pwdError = 'New password must differ from current password.';
      return;
    }

    this.changingPassword = true;
    this.users.changePassword(this.profile.userId, {
      currentPassword: this.pwdCurrent,
      newPassword:     this.pwdNew
    }).subscribe({
      next: () => {
        this.changingPassword = false;
        this.pwdCurrent = this.pwdNew = this.pwdConfirm = '';
        form.resetForm();
        this.toast.success('Password updated successfully');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.changingPassword = false;
        const msg = err?.error?.message || err?.error?.error || 'Could not change password.';
        this.pwdError = typeof msg === 'string' ? msg : 'Could not change password.';
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Wallet top-up ────────────────────────────────────────── */
  setQuickAmount(amount: number): void { this.topUpAmount = amount; }

  topUp(form: NgForm): void {
    if (!this.profile || this.toppingUp) return;
    if (!this.topUpAmount || this.topUpAmount < 1) {
      this.toast.warning('Enter a top-up amount of at least ₹1');
      return;
    }
    if (!/^[\w.\-]+@[\w.\-]+$/.test(this.topUpUpi)) {
      this.toast.warning('Enter a valid UPI ID (e.g. name@bank)');
      return;
    }

    this.toppingUp = true;
    this.wallet.topUp(this.profile.userId, {
      amount: this.topUpAmount,
      upiId:  this.topUpUpi
    }).subscribe({
      next: (res) => {
        if (this.profile) this.profile = { ...this.profile, walletBalance: res.walletBalance };
        this.toppingUp = false;
        const added = this.topUpAmount;
        this.topUpAmount = null;
        this.topUpUpi    = '';
        form.resetForm();
        this.toast.success(`Wallet topped up with ₹${added}`);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.toppingUp = false;
        this.toast.error(err?.error?.message || 'Top-up failed. Please try again.');
        this.cdr.detectChanges();
      }
    });
  }
}
