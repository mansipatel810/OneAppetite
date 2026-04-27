import {
  Component,
  OnInit,
  afterNextRender,
  inject,
  signal,
  computed,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginResponse } from '../services/auth.service';
import { FoodService, MenuItemResponse } from '../services/food.service';
import { MenuService, MenuItemPayload } from '../services/menu.service';
import { ToastService } from '../services/toast.service';
import { ThemeService, Theme } from '../services/theme.service';

type MealGroup = {
  course: string;
  items: MenuItemResponse[];
};

type MealCourse = 'Breakfast' | 'Lunch' | 'Dinner' | 'Snacks';
type Diet       = 'VEG' | 'NON_VEG';

interface ItemFormModel {
  itemName: string;
  category: string;
  mealCourse: MealCourse;
  dietaryType: Diet;
  price: number | null;
  quantityAvailable: number | null;
  imageUrl: string;
  minPrepTime: number | null;
}

@Component({
  selector: 'app-vendor-menu',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './vendor-menu.component.html',
  styleUrls: ['./vendor-menu.component.css'],
})
export class VendorMenuComponent implements OnInit {
  private authService = inject(AuthService);
  private foodService = inject(FoodService);
  private menuService = inject(MenuService);
  private toast       = inject(ToastService);
  private themeSvc    = inject(ThemeService);

  theme: Theme = 'light';
  private router      = inject(Router);
  private cdr         = inject(ChangeDetectorRef);

  user: LoginResponse | null = null;

  vendorId = signal<number | null>(null);
  items = signal<MenuItemResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  courseFilter = signal<'ALL' | MealCourse>('ALL');

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
    for (const c of order) {
      if (buckets.has(c)) {
        groups.push({ course: c, items: buckets.get(c)! });
        buckets.delete(c);
      }
    }
    for (const c of [...buckets.keys()].sort()) {
      groups.push({ course: c, items: buckets.get(c)! });
    }

    if (selected === 'ALL') return groups;
    return groups.filter((g) => g.course.toLowerCase() === selected.toLowerCase());
  });

  vendorName        = computed(() => this.items()[0]?.vendorName ?? this.user?.name ?? 'Vendor');
  vendorDescription = computed(() => this.items()[0]?.vendorDescription ?? '');
  vendorType        = computed(() => this.items()[0]?.vendorType ?? '');

  mobileMenuOpen = false;

  /* ── Modal state ─────────────────────────────────────────── */
  showFormModal   = signal<boolean>(false);
  formMode        = signal<'create' | 'edit'>('create');
  editingItemId   = signal<number | null>(null);
  saving          = signal<boolean>(false);

  showDeleteModal = signal<boolean>(false);
  deletingItem    = signal<MenuItemResponse | null>(null);
  deleting        = signal<boolean>(false);

  togglingStockId = signal<number | null>(null);

  readonly mealCourseOptions: MealCourse[] = ['Breakfast', 'Lunch', 'Dinner', 'Snacks'];
  readonly dietOptions: Diet[] = ['VEG', 'NON_VEG'];

  form: ItemFormModel = this.emptyForm();

  constructor() {
    afterNextRender(() => {
      setTimeout(() => {
        this.user = this.authService.getSession();
        if (!this.user || this.user.role !== 'VENDOR') {
          this.router.navigate(['/login']);
          return;
        }
        this.vendorId.set(this.user.userId);
        this.loadMenu(this.user.userId);
      }, 0);
    });
  }

  ngOnInit(): void {
    this.themeSvc.theme$.subscribe(t => {
      this.theme = t;
      this.cdr.detectChanges();
    });
  }

  toggleTheme(): void {
    this.themeSvc.toggle();
    this.toast.success(this.themeSvc.current === 'dark'
      ? 'Theme updated to Dark Mode'
      : 'Theme updated to Light Mode');
  }

  loadMenu(vendorId: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.foodService.getMenuItemsByVendor(vendorId).subscribe({
      next: (list) => {
        this.items.set(list ?? []);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e?.error?.error ?? e?.message ?? 'Failed to load menu.');
        this.loading.set(false);
      },
    });
  }

  setCourse(c: 'ALL' | MealCourse): void { this.courseFilter.set(c); }

  dietaryClass(d: string): string {
    const norm = (d ?? '').toUpperCase();
    if (norm.includes('NON')) return 'diet-badge nonveg';
    if (norm.includes('VEG')) return 'diet-badge veg';
    return 'diet-badge neutral';
  }

  initials(name: string): string {
    return (name ?? '')
      .split(' ').filter(Boolean).slice(0, 2)
      .map((p) => p[0]).join('').toUpperCase();
  }

  toggleMobileMenu(): void { this.mobileMenuOpen = !this.mobileMenuOpen; }
  closeMobileMenu(): void  { this.mobileMenuOpen = false; }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }

  dismissError(): void { this.error.set(null); }

  /* ── Add / Edit modal ────────────────────────────────────── */
  openCreateModal(): void {
    this.formMode.set('create');
    this.editingItemId.set(null);
    this.form = this.emptyForm();
    this.showFormModal.set(true);
  }

  openEditModal(item: MenuItemResponse): void {
    this.formMode.set('edit');
    this.editingItemId.set(item.itemId);
    this.form = {
      itemName:          item.itemName,
      category:          item.category,
      mealCourse:        (item.mealCourse as MealCourse) || 'Lunch',
      dietaryType:       (item.dietaryType as Diet)       || 'VEG',
      price:             item.price,
      quantityAvailable: item.quantityAvailable,
      imageUrl:          item.imageUrl ?? '',
      minPrepTime:       (item as any).minPrepTime ?? null,
    };
    this.showFormModal.set(true);
  }

  closeFormModal(): void {
    if (this.saving()) return;
    this.showFormModal.set(false);
  }

  submitForm(form: NgForm): void {
    if (form.invalid || !this.vendorId()) {
      this.toast.warning('Please correct the highlighted fields.');
      return;
    }
    if ((this.form.price ?? -1) < 0) {
      this.toast.warning('Price cannot be negative.');
      return;
    }
    if ((this.form.quantityAvailable ?? -1) < 0) {
      this.toast.warning('Quantity cannot be negative.');
      return;
    }

    const payload: MenuItemPayload = {
      itemName:          this.form.itemName.trim(),
      category:          this.form.category.trim(),
      mealCourse:        this.form.mealCourse,
      dietaryType:       this.form.dietaryType,
      price:             Number(this.form.price),
      quantityAvailable: Number(this.form.quantityAvailable),
      isInStock:         true, // new items default to in stock
      imageUrl:          this.form.imageUrl.trim() || undefined,
      minPrepTime:       this.form.minPrepTime != null ? Number(this.form.minPrepTime) : undefined,
    };

    this.saving.set(true);
    const vendorId = this.vendorId()!;

    const obs = this.formMode() === 'create'
      ? this.menuService.createMenuItem(vendorId, payload)
      : this.menuService.updateMenuItem(vendorId, this.editingItemId()!, payload);

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.showFormModal.set(false);
        this.toast.success(
          this.formMode() === 'create'
            ? `Added "${payload.itemName}" to your menu`
            : `Updated "${payload.itemName}"`
        );
        this.loadMenu(vendorId);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message || err?.error?.error || 'Could not save item.');
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Delete modal ────────────────────────────────────────── */
  openDeleteModal(item: MenuItemResponse): void {
    this.deletingItem.set(item);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    if (this.deleting()) return;
    this.showDeleteModal.set(false);
    this.deletingItem.set(null);
  }

  confirmDelete(): void {
    const item = this.deletingItem();
    const vendorId = this.vendorId();
    if (!item || !vendorId) return;

    this.deleting.set(true);
    this.menuService.deleteMenuItem(vendorId, item.itemId).subscribe({
      next: () => {
        this.deleting.set(false);
        this.showDeleteModal.set(false);
        this.toast.success(`Removed "${item.itemName}" from your menu`);
        this.deletingItem.set(null);
        this.loadMenu(vendorId);
      },
      error: () => {
        this.deleting.set(false);
        this.toast.error('Could not delete the item. Please try again.');
      }
    });
  }

  /* ── Stock toggle ────────────────────────────────────────── */
  toggleStock(item: MenuItemResponse): void {
    const vendorId = this.vendorId();
    if (!vendorId) return;
    if (this.togglingStockId() != null) return;

    const next = !item.isInStock;
    this.togglingStockId.set(item.itemId);

    // Optimistic flip
    this.items.update(list => list.map(
      i => i.itemId === item.itemId ? { ...i, isInStock: next } : i
    ));

    this.menuService.toggleStock(vendorId, item.itemId, next).subscribe({
      next: () => {
        this.togglingStockId.set(null);
        this.toast.success(`"${item.itemName}" is now ${next ? 'In Stock' : 'Out of Stock'}`);
      },
      error: () => {
        // Rollback
        this.items.update(list => list.map(
          i => i.itemId === item.itemId ? { ...i, isInStock: !next } : i
        ));
        this.togglingStockId.set(null);
        this.toast.error('Could not update stock. Please try again.');
      }
    });
  }

  isToggling(itemId: number): boolean {
    return this.togglingStockId() === itemId;
  }

  private emptyForm(): ItemFormModel {
    return {
      itemName: '',
      category: '',
      mealCourse: 'Lunch',
      dietaryType: 'VEG',
      price: null,
      quantityAvailable: null,
      imageUrl: '',
      minPrepTime: null,
    };
  }
}
