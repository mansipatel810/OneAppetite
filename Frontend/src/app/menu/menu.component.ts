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
import { SearchService } from '../services/search.service';
import { ToastService } from '../services/toast.service';

type MealCourse = 'All' | 'Breakfast' | 'Lunch' | 'Dinner';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit, OnDestroy {
  private route    = inject(ActivatedRoute);
  private router   = inject(Router);
  private location = inject(Location);
  private menuSvc   = inject(MenuService);
  private authSvc   = inject(AuthService);
  private cartSvc   = inject(CartService);
  private searchSvc = inject(SearchService);
  private toast     = inject(ToastService);
  private cdr       = inject(ChangeDetectorRef);

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

  /** ALL | VEG | NON_VEG dietary filter — combines with search + meal + category */
  dietaryFilter: 'ALL' | 'VEG' | 'NON_VEG' = 'ALL';

  searchText = '';
  isLoading  = true;
  hasError   = false;

  cartState!: CartState;
  private cartSub?: Subscription;
  private searchSub?: Subscription;

  get cartCount(): number {
    let n = 0;
    this.cartState?.itemMap?.forEach(ci => n += ci.quantity);
    return n;
  }

  get cartTotal(): number {
    return this.cartState?.totalAmount ?? 0;
  }

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('vendorId');
    this.vendorId = Number(raw);

    if (!raw || !Number.isFinite(this.vendorId) || this.vendorId <= 0) {
      // Bad / missing vendorId param → bail out gracefully
      this.hasError = true;
      this.isLoading = false;
      this.toast.error('Invalid vendor link.');
      this.cdr.detectChanges();
      return;
    }

    const session = this.authSvc.getSession();
    this.userId   = session?.userId ?? 0;

    // Reset any stale search term left over from prior pages so we always
    // start with the full menu visible. Without this, typing "Paneer" on
    // one screen filters out every item on the next vendor's menu.
    this.searchSvc.clear();
    this.searchText = '';

    // Mirror cart BehaviorSubject
    this.cartSub = this.cartSvc.cart$.subscribe(state => {
      this.cartState = state;
      this.cdr.detectChanges();
    });

    // Mirror navbar search term — filter menu list as user types
    this.searchSub = this.searchSvc.term$.subscribe(term => {
      this.searchText = (term ?? '').toLowerCase();
      this.applyFilters();
    });

    // Cart is already loaded by SidebarComponent on app init.
    // Only load here if the map is empty (e.g. deep-link directly to this page).
    if ((this.cartSvc.snapshot.itemMap?.size ?? 0) === 0) {
      this.cartSvc.loadCart(this.userId);
    }

    this.loadMenu();
  }

  ngOnDestroy(): void {
    this.cartSub?.unsubscribe();
    this.searchSub?.unsubscribe();
    this.searchSvc.clear();
  }

  /* ── Menu loading ─────────────────────────────────────────────── */
  private loadMenu(): void {
    this.isLoading = true;
    this.hasError  = false;

    this.menuSvc.getMenuItems(this.vendorId).subscribe({
      next: (items) => {
        const list = items ?? [];
        this.allItems   = list;
        this.categories = [
          'All',
          ...Array.from(new Set(list.map(i => i.category).filter(Boolean)))
        ];

        // Derive vendor identity from the response itself — backend
        // returns vendorName / vendorDescription on each MenuItemResponse,
        // so we don't need a separate vendor lookup.
        const first = list[0];
        if (first) {
          this.vendorName        = first.vendorName ?? '';
          this.vendorDescription = first.vendorDescription ?? '';
        }

        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[MenuComponent] Failed to load menu', err);
        this.hasError  = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /* ── Filters ──────────────────────────────────────────────────── */
  setCategory(cat: string): void     { this.activeCategory   = cat; this.applyFilters(); }
  setMealCourse(c: MealCourse): void { this.activeMealCourse = c;   this.applyFilters(); }
  setDietaryFilter(d: 'ALL' | 'VEG' | 'NON_VEG'): void {
    this.dietaryFilter = d;
    this.applyFilters();
  }

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
    if (this.dietaryFilter !== 'ALL') {
      list = list.filter(i => {
        const dt = (i.dietaryType || '').toUpperCase().replace('-', '_');
        if (this.dietaryFilter === 'VEG')     return dt.includes('VEG') && !dt.includes('NON');
        if (this.dietaryFilter === 'NON_VEG') return dt.includes('NON');
        return true;
      });
    }
    if (this.searchText)
      list = list.filter(i => i.itemName.toLowerCase().includes(this.searchText));
    this.filteredItems = list;
    this.cdr.detectChanges();
  }

  /* ── Cart ────────────────────────────────────────────────────── */
  qtyOf(itemId: number): number {
    return this.cartState?.itemMap?.get(itemId)?.quantity ?? 0;
  }

  addToCart(item: MenuItem): void {
    if (!item.isInStock) {
      this.toast.warning(`${item.itemName} is out of stock`);
      return;
    }
    this.cartSvc.addItem({ userId: this.userId, menuItemId: item.itemId, quantity: 1 });
    this.toast.success(`Added ${item.itemName} to cart`);
  }

  removeFromCart(item: MenuItem): void {
    const entry = this.cartState?.itemMap?.get(item.itemId);
    if (!entry) return;
    this.cartSvc.reduceItem(this.userId, item.itemId, entry.orderItemId);
  }

  isVeg(item: MenuItem):    boolean { return item.dietaryType?.toUpperCase() === 'VEG'; }
  isNonVeg(item: MenuItem): boolean { return item.dietaryType?.toUpperCase() === 'NON_VEG'; }

  /**
   * Back navigation fix:
   * Always go to /dashboard (the vendor list page), NOT history.back()
   * which could land on the city/campus selection screen.
   */
  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}