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
import { AdminService, UserResponse } from '../../services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
})
export class AdminDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private adminService = inject(AdminService);
  private router = inject(Router);

  user: LoginResponse | null = null;

  users = signal<UserResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  mobileMenuOpen = false;

  // Managed population excludes ADMIN — same convention as admin-users
  private managedUsers = computed(() =>
    this.users().filter((u) => u.role !== 'ADMIN')
  );

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

  initials(name: string): string {
    return (name ?? '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0])
      .join('')
      .toUpperCase();
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
