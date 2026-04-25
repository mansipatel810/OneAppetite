import { Component, OnInit, ChangeDetectorRef, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { UserSettingsService, UserSettings } from '../services/user-settings.service';

@Component({
  selector: 'app-user-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './user-settings.component.html',
})
export class UserSettingsComponent implements OnInit {
  settings: UserSettings | null = null;
  userId!: number;
  loading = true;
  error = '';
  successMessage = '';

  // Wallet top-up
  topUpAmount: number = 100;
  topUpOptions = [100, 200, 500, 1000];

  // Dietary options
  dietaryOptions = ['VEG', 'NON_VEG', 'JAIN'];

  favorites: any[] = [];
  constructor(
    private authService: AuthService,
    private userSettingsService: UserSettingsService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const session = this.authService.getSession();
    if (!session) {
      this.router.navigate(['/login']);
      return;
    }
    this.userId = session.userId;
    this.loadSettings();
    this.loadFavorites();
  }

  loadSettings(): void {
    this.userSettingsService.getSettings(this.userId).subscribe({
      next: (data) => {
        setTimeout(() => {
          this.settings = data;
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        setTimeout(() => {
          this.error = 'Failed to load settings.';
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  loadFavorites(): void {
    this.userSettingsService.getFavorites(this.userId).subscribe({
      next: (items) => {
        setTimeout(() => {
          this.favorites = items;
          this.cdr.detectChanges();
        });
      }
    });
  }

  removeFavorite(itemId: number): void {
    this.userSettingsService.removeFavorite(this.userId, itemId).subscribe({
      next: () => {
        setTimeout(() => {
          this.favorites = this.favorites.filter(f => f.itemId !== itemId);
          this.showSuccess('Removed from favorites.');
          this.cdr.detectChanges();
        });
      }
    });
  }

  reOrder(item: any): void {
    this.router.navigate(['/vendor', item.vendorId]);
  }

  setDietaryPreference(preference: string): void {
    if (!this.settings) return;
    // Toggle off if already selected
    const newValue = this.settings.dietaryPreference === preference ? null : preference;
    this.userSettingsService.updateSettings(this.userId, { dietaryPreference: newValue }).subscribe({
      next: (updated) => {
        setTimeout(() => {
          this.settings = updated;
          this.showSuccess('Dietary preference updated!');
          this.cdr.detectChanges();
        });
      }
    });
  }

  toggleNotifications(): void {
    if (!this.settings) return;
    this.userSettingsService.updateSettings(this.userId, {
      notificationsEnabled: !this.settings.notificationsEnabled
    }).subscribe({
      next: (updated) => {
        setTimeout(() => {
          this.settings = updated;
          this.showSuccess('Notification preference updated!');
          this.cdr.detectChanges();
        });
      }
    });
  }

  topUpWallet(amount: number): void {
    this.userSettingsService.topUpWallet(this.userId, amount).subscribe({
      next: (updated) => {
        setTimeout(() => {
          this.settings = updated;
          this.showSuccess(`₹${amount} added to your wallet!`);
          this.cdr.detectChanges();
        });
      }
    });
  }

  showSuccess(message: string): void {
    this.successMessage = message;
    setTimeout(() => {
      this.successMessage = '';
      this.cdr.detectChanges();
    }, 3000);
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }
}