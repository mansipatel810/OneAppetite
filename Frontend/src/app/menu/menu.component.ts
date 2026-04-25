import {
  Component, OnInit, OnDestroy,
  ChangeDetectorRef, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location } from '@angular/common';
import { Subscription } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { MenuService, MenuItem } from '../services/menu.service';
import { CartService, CartState } from '../services/cart.service';
import { UserSettingsService } from '../services/user-settings.service';

type MealCourse = 'All' | 'Breakfast' | 'Lunch' | 'Dinner';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit, OnDestroy {
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);
  private location    = inject(Location);
  private menuSvc     = inject(MenuService);
  private authSvc     = inject(AuthService);
  private cartSvc     = inject(CartService);
  private cdr         = inject(ChangeDetectorRef);
  private settingsSvc = inject(UserSettingsService);

  vendorId!: number;
  userId!: number;

  vendorName        = '';
  vendorDescription = '';

  allItems:      MenuItem[] = [];
  filteredItems: MenuItem[] = [];
  categories:    string[]   = [];

  activeCategory:   string     = 'All';
  activeMealCourse: MealCourse = 'All';
  readonly mealCourses: MealCourse[] = ['All', 'Breakfast', 'Lunch', 'Dinner'];

  dietaryFilter: string | null = null;
  favoriteIds = new Set<number>();

  searchText = '';
  isLoading  = true;
  hasError   = false;

  cartState!: CartState;
  private cartSub?: Subscription;

  get cartCount(): number {
    let n = 0;
    this.cartState?.itemMap?.forEach(ci => n += ci.quantity);
    return n;
  }

  get cartTotal(): number {
    return this.cartState?.totalAmount ?? 0;
  }

  ngOnInit(): void {
    this.vendorId = Number(this.route.snapshot.paramMap.get('vendorId'));
    const session = this.authSvc.getSession();
    this.userId   = session?.userId ?? 0;

    // Mirror cart BehaviorSubject
    this.cartSub = this.cartSvc.cart$.subscribe(state => {
      this.cartState = state;
      this.cdr.detectChanges();
    });

    // Cart is already loaded by SidebarComponent on app init.
    // Only load here if the map is empty (e.g. deep-link directly to this page).
    if ((this.cartSvc.snapshot.itemMap?.size ?? 0) === 0) {
      this.cartSvc.loadCart(this.userId);
    }

    if (this.userId) {
      // Load settings and favorites in parallel
      this.settingsSvc.getSettings(this.userId).subscribe({
        next: (s) => {
          this.dietaryFilter = s.dietaryPreference;
          this.loadMenu();
        },
        error: () => this.loadMenu()
      });

      this.settingsSvc.getFavorites(this.userId).subscribe({
        next: (items) => {
          items.forEach(i => this.favoriteIds.add(i.itemId));
          this.cdr.detectChanges();
        },
        error: () => {}
      });

    } else {
      this.loadMenu();
    }
  }

  ngOnDestroy(): void { this.cartSub?.unsubscribe(); }

  /* ── Menu loading ─────────────────────────────────────────────── */
  private loadMenu(): void {
    this.isLoading = true;
    this.hasError  = false;

    this.menuSvc.getMenuItems(this.vendorId).subscribe({
      next: (items) => {
        this.allItems   = items;
        this.categories = [
          'All',
          ...Array.from(new Set(items.map(i => i.category).filter(Boolean)))
        ];
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.hasError  = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });

    this.menuSvc.getVendorById(this.vendorId).subscribe({
      next: (v) => {
        this.vendorName        = v?.vendorName || v?.name || '';
        this.vendorDescription = v?.vendorDescription || '';
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  /* ── Filters ──────────────────────────────────────────────────── */
  setCategory(cat: string): void     { this.activeCategory   = cat; this.applyFilters(); }
  setMealCourse(c: MealCourse): void { this.activeMealCourse = c;   this.applyFilters(); }

  onSearch(event: Event): void {
    this.searchText = (event.target as HTMLInputElement).value.toLowerCase();
    this.applyFilters();
  }

  private applyFilters(): void {
    let list = this.allItems;
    if (this.activeMealCourse !== 'All')
      list = list.filter(i => (i as any).mealCourse?.toLowerCase() === this.activeMealCourse.toLowerCase());
    if (this.activeCategory !== 'All')
      list = list.filter(i => i.category === this.activeCategory);
    if (this.searchText)
      list = list.filter(i => i.itemName.toLowerCase().includes(this.searchText));
    if (this.dietaryFilter) {
      const normalize = (val: string) =>
        val.toLowerCase().replace(/[\s\-]/g, '_');  // converts "Non-Veg", "non veg" etc. to "non_veg"

      const filterNorm = normalize(this.dietaryFilter);
      list = list.filter(i =>
        i.dietaryType ? normalize(i.dietaryType) === filterNorm : false
      );
    }

    this.filteredItems = list;
    this.cdr.detectChanges();
  }

  /* ── Favorites ───────────────────────────────────────────────── */
  isFav(item: MenuItem): boolean {
    return this.favoriteIds.has(item.itemId);
  }

  toggleFavorite(item: MenuItem): void {
    if (this.isFav(item)) {
      this.settingsSvc.removeFavorite(this.userId, item.itemId).subscribe({
        next: () => {
          this.favoriteIds.delete(item.itemId);
          this.cdr.detectChanges();
        },
        error: () => {}
      });
    } else {
      this.settingsSvc.addFavorite(this.userId, item.itemId).subscribe({
        next: () => {
          this.favoriteIds.add(item.itemId);
          this.cdr.detectChanges();
        },
        error: () => {}
      });
    }
  }

  /* ── Cart ────────────────────────────────────────────────────── */
  qtyOf(itemId: number): number {
    return this.cartState?.itemMap?.get(itemId)?.quantity ?? 0;
  }

  addToCart(item: MenuItem): void {
    if (!item.isInStock) return;
    this.cartSvc.addItem({ userId: this.userId, menuItemId: item.itemId, quantity: 1 });
  }

  removeFromCart(item: MenuItem): void {
    const entry = this.cartState?.itemMap?.get(item.itemId);
    if (!entry) return;
    this.cartSvc.reduceItem(this.userId, item.itemId, entry.orderItemId);
  }

  isVeg(item: MenuItem):    boolean { return item.dietaryType?.toLowerCase().replace(/[\s\-]/g, '_') === 'veg'; }
  isNonVeg(item: MenuItem): boolean { return item.dietaryType?.toLowerCase().replace(/[\s\-]/g, '_') === 'non_veg' }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}