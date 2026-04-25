import {
  Component,
  OnInit,
  afterNextRender,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService, LoginResponse } from '../../services/auth.service';
import {
  AdminService,
  MenuItemResponse,
} from '../../services/admin.service';

@Component({
  selector: 'app-admin-vendor-menu',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-vendor-menu.component.html',
  styleUrls: ['./admin-vendor-menu.component.css'],
})
export class AdminVendorMenuComponent implements OnInit {
  private authService = inject(AuthService);
  private adminService = inject(AdminService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  user: LoginResponse | null = null;

  vendorId = signal<number | null>(null);
  items = signal<MenuItemResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  // Course filter for the read-only view
  courseFilter = signal<'ALL' | 'Breakfast' | 'Lunch' | 'Dinner'>('ALL');

  mobileMenuOpen = false;

  // Vendor identity inferred from the first item; falls back when menu is empty
  vendorName = computed(() => this.items()[0]?.vendorName ?? `Vendor #${this.vendorId() ?? ''}`);
  vendorDescription = computed(() => this.items()[0]?.vendorDescription ?? '');
  vendorType = computed(() => this.items()[0]?.vendorType ?? '');

  filteredItems = computed(() => {
    const list = this.items();
    const course = this.courseFilter();
    if (course === 'ALL') return list;
    return list.filter(
      (i) => (i.mealCourse ?? '').toLowerCase() === course.toLowerCase()
    );
  });

  // Stat row over the unfiltered menu
  stats = computed(() => {
    const list = this.items();
    return {
      total: list.length,
      inStock: list.filter((i) => i.isInStock).length,
      outOfStock: list.filter((i) => !i.isInStock).length,
      veg: list.filter((i) =>
        (i.dietaryType ?? '').toUpperCase().startsWith('VEG')
      ).length,
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

        const idParam = this.route.snapshot.paramMap.get('vendorId');
        const id = idParam ? Number(idParam) : NaN;
        if (!id || Number.isNaN(id)) {
          this.error.set('Invalid vendor id.');
          this.loading.set(false);
          return;
        }
        this.vendorId.set(id);
        this.loadMenu(id);
      }, 0);
    });
  }

  ngOnInit(): void {}

  loadMenu(vendorId: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getVendorMenu(vendorId).subscribe({
      next: (list) => {
        this.items.set(list ?? []);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e?.message ?? 'Failed to load vendor menu.');
        this.loading.set(false);
      },
    });
  }

  setCourse(c: 'ALL' | 'Breakfast' | 'Lunch' | 'Dinner'): void {
    this.courseFilter.set(c);
  }

  dietaryClass(d: string): string {
    const norm = (d ?? '').toUpperCase();
    if (norm.includes('NON')) return 'diet-badge nonveg';
    if (norm.includes('VEG')) return 'diet-badge veg';
    return 'diet-badge neutral';
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

  back(): void {
    this.router.navigate(['/admin/users']);
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
