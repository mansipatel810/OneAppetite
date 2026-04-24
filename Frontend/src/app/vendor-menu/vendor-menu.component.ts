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
import { AuthService, LoginResponse } from '../services/auth.service';
import { FoodService, MenuItemResponse } from '../services/food.service';

type MealGroup = {
  course: string;            // display label, e.g. "Breakfast"
  items: MenuItemResponse[];
};

@Component({
  selector: 'app-vendor-menu',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './vendor-menu.component.html',
  styleUrls: ['./vendor-menu.component.css'],
})
export class VendorMenuComponent implements OnInit {
  private authService = inject(AuthService);
  private foodService = inject(FoodService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  user: LoginResponse | null = null;

  vendorId = signal<number | null>(null);
  items = signal<MenuItemResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  // Course filter: 'ALL' shows every course
  courseFilter = signal<'ALL' | 'Breakfast' | 'Lunch' | 'Dinner'>('ALL');

  // Group by meal course for the list view
  groupedItems = computed<MealGroup[]>(() => {
    const list = this.items();
    const selected = this.courseFilter();
    const order = ['Breakfast', 'Lunch', 'Dinner'];

    const buckets = new Map<string, MenuItemResponse[]>();
    for (const it of list) {
      const course = (it.mealCourse || 'Other').trim();
      if (!buckets.has(course)) buckets.set(course, []);
      buckets.get(course)!.push(it);
    }

    const groups: MealGroup[] = [];

    // Known courses first, in canonical order
    for (const c of order) {
      if (buckets.has(c)) {
        groups.push({ course: c, items: buckets.get(c)! });
        buckets.delete(c);
      }
    }
    // Anything else (e.g. "Snacks") appended in alpha order
    for (const c of [...buckets.keys()].sort()) {
      groups.push({ course: c, items: buckets.get(c)! });
    }

    if (selected === 'ALL') return groups;
    return groups.filter((g) => g.course.toLowerCase() === selected.toLowerCase());
  });

  // Vendor identity — taken from the first item. If the vendor has no items yet,
  // we still render the page with an empty state and fall back to "Vendor".
  vendorName = computed(() => this.items()[0]?.vendorName ?? 'Vendor');
  vendorDescription = computed(() => this.items()[0]?.vendorDescription ?? '');
  vendorType = computed(() => this.items()[0]?.vendorType ?? '');

  // Mobile sidebar
  mobileMenuOpen = false;

  constructor() {
    afterNextRender(() => {
      setTimeout(() => {
        this.user = this.authService.getSession();
        if (!this.user) {
          this.router.navigate(['/login']);
          return;
        }

        const idParam = this.route.snapshot.paramMap.get('id');
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

    this.foodService.getMenuItemsByVendor(vendorId).subscribe({
      next: (list) => {
        this.items.set(list ?? []);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(
          e?.error?.error ?? e?.message ?? 'Failed to load menu.'
        );
        this.loading.set(false);
      },
    });
  }

  setCourse(c: 'ALL' | 'Breakfast' | 'Lunch' | 'Dinner'): void {
    this.courseFilter.set(c);
  }

  // Badge colour helper for dietary type
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
    this.router.navigate(['/dashboard']);
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
