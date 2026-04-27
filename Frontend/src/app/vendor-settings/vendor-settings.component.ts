import {
  Component, OnInit, ChangeDetectorRef, inject, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginResponse } from '../services/auth.service';
import {
  UserService, UserProfile,
  VendorLocationsResponse, VendorLocationBuilding
} from '../services/user.service';
import { FoodService } from '../services/food.service';
import { ToastService } from '../services/toast.service';
import { ThemeService, Theme } from '../services/theme.service';

@Component({
  selector: 'app-vendor-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './vendor-settings.component.html',
  styleUrls: ['./vendor-settings.component.css']
})
export class VendorSettingsComponent implements OnInit {
  private auth     = inject(AuthService);
  private users    = inject(UserService);
  private foodSvc  = inject(FoodService);
  private toast    = inject(ToastService);
  private themeSvc = inject(ThemeService);
  private router   = inject(Router);
  private cdr      = inject(ChangeDetectorRef);

  user: LoginResponse | null = null;
  theme: Theme = 'light';

  isLoading = signal(true);
  hasError  = signal(false);
  profile = signal<UserProfile | null>(null);

  // Profile edit
  editName  = '';
  editPhone = '';
  savingProfile = signal(false);

  // Notifications
  togglingNotifs = signal(false);

  // Password
  pwdCurrent = '';
  pwdNew     = '';
  pwdConfirm = '';
  changingPassword = signal(false);
  pwdError = signal<string | null>(null);

  /* ── Stall Locations ─────────────────────────────────── */
  locations = signal<VendorLocationsResponse | null>(null);
  loadingLocations = signal(true);
  savingLocations = signal(false);

  // Add-location modal state
  addModalOpen = signal(false);
  cities: any[] = [];
  campuses: any[] = [];
  addBuildings: any[] = [];
  selCityId: number | null = null;
  selCityName = '';
  selCampusId: number | null = null;
  selCampusName = '';
  selBuildingId: number | null = null;
  selBuildingName = '';

  ngOnInit(): void {
    const session = this.auth.getSession();
    if (!session?.userId || session.role !== 'VENDOR') {
      this.router.navigate(['/login']);
      return;
    }
    this.user = session;

    this.themeSvc.theme$.subscribe(t => { this.theme = t; this.cdr.detectChanges(); });

    this.loadProfile(session.userId);
    this.loadLocations(session.userId);
  }

  /* ── Theme + nav ─────────────────────────────────────── */
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

  /* ── Profile load ────────────────────────────────────── */
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

  /* ── Notifications ───────────────────────────────────── */
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

  /* ── Password ────────────────────────────────────────── */
  changePassword(form: NgForm): void {
    const p = this.profile();
    if (!p) return;
    this.pwdError.set(null);

    if (this.pwdNew.length < 6) {
      this.pwdError.set('New password must be at least 6 characters.');
      return;
    }
    if (this.pwdNew !== this.pwdConfirm) {
      this.pwdError.set('New password and confirmation do not match.');
      return;
    }
    if (this.pwdNew === this.pwdCurrent) {
      this.pwdError.set('New password must differ from current password.');
      return;
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

  /* ── Stall Locations ─────────────────────────────────── */
  private loadLocations(userId: number): void {
    this.loadingLocations.set(true);
    this.users.getVendorLocations(userId).subscribe({
      next: (res) => {
        this.locations.set(res);
        this.loadingLocations.set(false);
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingLocations.set(false);
        this.toast.error('Could not load your stall locations.');
        this.cdr.detectChanges();
      }
    });
  }

  removeAdditional(building: VendorLocationBuilding): void {
    const p = this.profile();
    const loc = this.locations();
    if (!p || !loc || this.savingLocations()) return;

    const remaining = loc.additionalBuildings
      .filter(b => b.buildingId !== building.buildingId)
      .map(b => b.buildingId);

    this.persistLocations(p.userId, remaining,
      `Removed ${building.buildingName}`);
  }

  private persistLocations(userId: number, ids: number[], successMsg: string): void {
    this.savingLocations.set(true);
    this.users.updateVendorLocations(userId, ids).subscribe({
      next: (res) => {
        this.locations.set(res);
        this.savingLocations.set(false);
        this.toast.success(successMsg);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.savingLocations.set(false);
        this.toast.error(err?.error?.message || 'Could not update locations.');
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Add Location modal ──────────────────────────────── */
  openAddModal(): void {
    this.resetAddSelection();
    this.addModalOpen.set(true);
    if (this.cities.length === 0) {
      this.foodSvc.getCities().subscribe({
        next: (data) => { this.cities = data; this.cdr.detectChanges(); },
        error: () => this.toast.error('Could not load cities.')
      });
    }
  }
  closeAddModal(): void {
    if (this.savingLocations()) return;
    this.addModalOpen.set(false);
  }

  onAddCityChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const id = select.value ? Number(select.value) : null;
    this.selCityId = id;
    this.selCityName = select.options[select.selectedIndex]?.text || '';
    this.campuses = [];
    this.addBuildings = [];
    this.selCampusId = null; this.selCampusName = '';
    this.selBuildingId = null; this.selBuildingName = '';
    if (id) {
      this.foodSvc.getCampuses(id).subscribe({
        next: (data) => { this.campuses = data; this.cdr.detectChanges(); }
      });
    }
  }

  onAddCampusChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const id = select.value ? Number(select.value) : null;
    this.selCampusId = id;
    this.selCampusName = select.options[select.selectedIndex]?.text || '';
    this.addBuildings = [];
    this.selBuildingId = null; this.selBuildingName = '';
    if (id) {
      this.foodSvc.getBuildings(id).subscribe({
        next: (data) => { this.addBuildings = data; this.cdr.detectChanges(); }
      });
    }
  }

  onAddBuildingChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const id = select.value ? Number(select.value) : null;
    this.selBuildingId = id;
    this.selBuildingName = select.options[select.selectedIndex]?.text || '';
  }

  confirmAddLocation(): void {
    const p = this.profile();
    const loc = this.locations();
    if (!p || !loc || !this.selBuildingId) return;

    if (loc.primaryBuilding?.buildingId === this.selBuildingId) {
      this.toast.warning('That is already your primary location.');
      return;
    }
    if (loc.additionalBuildings.some(b => b.buildingId === this.selBuildingId)) {
      this.toast.warning('You already serve that building.');
      return;
    }

    const ids = [
      ...loc.additionalBuildings.map(b => b.buildingId),
      this.selBuildingId
    ];
    this.persistLocations(p.userId, ids, `Now serving ${this.selBuildingName}`);
    this.closeAddModal();
  }

  private resetAddSelection(): void {
    this.selCityId = null; this.selCityName = '';
    this.selCampusId = null; this.selCampusName = '';
    this.selBuildingId = null; this.selBuildingName = '';
    this.campuses = [];
    this.addBuildings = [];
  }
}
