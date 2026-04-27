import {
  Component, EventEmitter, Output, ChangeDetectorRef, inject, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

type Step = 'phone' | 'otp' | 'reset' | 'done';

@Component({
  selector: 'app-forgot-password-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot-password-modal.component.html',
  styleUrls: ['./forgot-password-modal.component.css']
})
export class ForgotPasswordModalComponent {
  @Output() closed = new EventEmitter<void>();
  @Output() resetCompleted = new EventEmitter<void>();

  private auth  = inject(AuthService);
  private toast = inject(ToastService);
  private cdr   = inject(ChangeDetectorRef);

  step = signal<Step>('phone');

  // Form state
  phone = '';
  otp = '';
  newPassword = '';
  confirmPassword = '';
  showPassword = false;

  // Async state
  busy = signal(false);
  error = signal<string | null>(null);

  // OTP TTL banner
  expiresInMinutes = 10;

  /* ── Step 1: send OTP ──────────────────────────────────── */
  sendOtp(form: NgForm): void {
    if (form.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);

    this.auth.forgotPassword({ phone: this.phone.trim() }).subscribe({
      next: (res) => {
        this.busy.set(false);
        this.step.set('otp');
        if (res?.expiresInMinutes) this.expiresInMinutes = Number(res.expiresInMinutes);
        this.toast.success('OTP sent to your registered number');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.busy.set(false);
        const msg = err?.message || 'Could not send OTP. Please try again.';
        this.error.set(msg);
        this.toast.error(msg);
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Step 2: verify OTP ────────────────────────────────── */
  verifyOtp(form: NgForm): void {
    if (form.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);

    this.auth.verifyOtp({ phone: this.phone.trim(), otp: this.otp.trim() }).subscribe({
      next: () => {
        this.busy.set(false);
        this.step.set('reset');
        this.toast.success('OTP verified — set a new password.');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.busy.set(false);
        const msg = err?.message || 'Invalid or expired OTP.';
        this.error.set(msg);
        this.toast.error(msg);
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Step 3: reset password ────────────────────────────── */
  resetPassword(form: NgForm): void {
    if (this.busy()) return;
    this.error.set(null);

    if (this.newPassword.length < 8) {
      this.error.set('Password must be at least 8 characters.');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.error.set('Passwords do not match.');
      return;
    }

    this.busy.set(true);
    this.auth.resetPassword({
      phone: this.phone.trim(),
      otp:   this.otp.trim(),
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.busy.set(false);
        this.step.set('done');
        this.toast.success('Password reset successfully');
        this.cdr.detectChanges();

        // Give the user a moment to see the success state before closing
        setTimeout(() => {
          this.resetCompleted.emit();
          this.close();
        }, 1400);
      },
      error: (err) => {
        this.busy.set(false);
        const msg = err?.message || 'Could not reset password. Please try again.';
        this.error.set(msg);
        this.toast.error(msg);
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Helpers ───────────────────────────────────────────── */
  resendOtp(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.auth.forgotPassword({ phone: this.phone.trim() }).subscribe({
      next: () => {
        this.busy.set(false);
        this.toast.success('A new OTP has been sent.');
        this.otp = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.busy.set(false);
        this.toast.error('Could not resend OTP.');
        this.cdr.detectChanges();
      }
    });
  }

  togglePassword(): void { this.showPassword = !this.showPassword; }

  backToPhone(): void { this.step.set('phone'); this.error.set(null); }

  close(): void {
    if (this.busy()) return;
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.close();
  }
}
