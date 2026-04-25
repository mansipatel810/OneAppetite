import {
  Component,
  OnInit,
  afterNextRender,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginResponse } from '../../services/auth.service';
import {
  AdminService,
  UserResponse,
} from '../../services/admin.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.css'],
})
export class AdminUsersComponent implements OnInit {
  private authService = inject(AuthService);
  private adminService = inject(AdminService);
  private router = inject(Router);

  user: LoginResponse | null = null;

  // Reactive state (signals) — matches Login/Register convention
  users = signal<UserResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  togglingId = signal<number | null>(null);

  // UI filter state — admins are hidden from this screen by design
  roleFilter = signal<'ALL' | 'EMPLOYEE' | 'VENDOR'>('ALL');
  searchTerm = signal<string>('');

  // Mobile sidebar
  mobileMenuOpen = false;

  /**
   * Managed users — always excludes ADMIN. Admins manage each other out of band;
   * this screen is for overseeing cafeteria access (employees + vendors only).
   */
  managedUsers = computed(() =>
    this.users().filter((u) => u.role !== 'ADMIN')
  );

  // Derived view list (filter + search) over the managed (non-admin) population
  filteredUsers = computed(() => {
    const list = this.managedUsers();
    const role = this.roleFilter();
    const term = this.searchTerm().trim().toLowerCase();

    return list.filter((u) => {
      if (role !== 'ALL' && u.role !== role) return false;
      if (!term) return true;
      return (
        u.name.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
      );
    });
  });

  // Summary counts for the stat cards — over managed users only
  stats = computed(() => {
    const list = this.managedUsers();
    return {
      total: list.length,
      employees: list.filter((u) => u.role === 'EMPLOYEE').length,
      vendors: list.filter((u) => u.role === 'VENDOR').length,
      inactive: list.filter((u) => !u.isActive).length,
    };
  });

  constructor() {
    afterNextRender(() => {
      setTimeout(() => {
        this.user = this.authService.getSession();
        if (!this.user) {
          this.router.navigate(['/login']);
          return;
        }
        if (this.user.role !== 'ADMIN') {
          this.router.navigate(['/dashboard']);
          return;
        }
        this.loadUsers();
      }, 0);
    });
  }

  ngOnInit(): void {}

  loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getAllUsers().subscribe({
      next: (list) => {
        this.users.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e?.message ?? 'Failed to load users.');
        this.loading.set(false);
      },
    });
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm.set(value);
  }

  setRoleFilter(role: 'ALL' | 'EMPLOYEE' | 'VENDOR'): void {
    this.roleFilter.set(role);
  }

  /** Navigate to the read-only vendor menu view. */
  viewMenu(u: UserResponse): void {
    if (u.role !== 'VENDOR') return;
    this.router.navigate(['/admin/vendors', u.userId, 'menu']);
  }

  /** Flip a user's active flag. Blocks self-toggle to prevent lockout. */
  toggle(u: UserResponse): void {
    if (this.user && u.userId === this.user.userId) {
      return; // can't toggle yourself
    }
    if (this.togglingId() === u.userId) {
      return; // already in-flight
    }

    this.togglingId.set(u.userId);

    this.adminService.toggleUserStatus(u.userId).subscribe({
      next: (updated) => {
        this.users.update((arr) =>
          arr.map((x) => (x.userId === updated.userId ? updated : x))
        );
        this.togglingId.set(null);
      },
      error: (e) => {
        this.error.set(e?.message ?? 'Failed to update user status.');
        this.togglingId.set(null);
      },
    });
  }

  // Helper for avatar initials
  initials(name: string): string {
    return (name ?? '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0])
      .join('')
      .toUpperCase();
  }

  // Role badge CSS class
  roleClass(role: string): string {
    switch (role) {
      case 'ADMIN':
        return 'badge-role admin';
      case 'VENDOR':
        return 'badge-role vendor';
      case 'EMPLOYEE':
      default:
        return 'badge-role employee';
    }
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }

  dismissError(): void {
    this.error.set(null);
  }
}
