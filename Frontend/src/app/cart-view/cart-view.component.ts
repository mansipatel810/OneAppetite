import {
  Component, OnInit, OnDestroy,
  ChangeDetectorRef, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { CartService, CartState, CartItemDTO } from '../services/cart.service';

@Component({
  selector: 'app-cart-view',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart-view.component.html',
  styleUrls: ['./cart-view.component.css']
})
export class CartViewComponent implements OnInit, OnDestroy {
  private authSvc  = inject(AuthService);
  private cartSvc  = inject(CartService);
  private router   = inject(Router);
  private cdr      = inject(ChangeDetectorRef);

  userId!: number;
  cartState!: CartState;
  isLoading  = true;
  hasError   = false;

  private sub?: Subscription;

  get items(): CartItemDTO[] {
    const arr: CartItemDTO[] = [];
    this.cartState?.itemMap.forEach(ci => arr.push(ci));
    return arr;
  }

  get isEmpty(): boolean {
    return !this.isLoading && (this.cartState?.itemMap.size ?? 0) === 0;
  }

  get vendorName(): string {
    const v = this.cartState?.vendor;
    return v?.vendorName || v?.name || 'Vendor';
  }

  get readyTimeFormatted(): string {
    const rt = this.cartState?.readyTime;
    if (!rt) return '—';
    try {
      return new Date(rt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch { return rt; }
  }

  ngOnInit(): void {
    const session = this.authSvc.getSession();
    this.userId   = session?.userId ?? 0;

    // Subscribe to shared cart state (already loaded by MenuComponent or re-load here)
    this.sub = this.cartSvc.cart$.subscribe(state => {
      this.cartState = state;
      this.isLoading = false;
      this.cdr.detectChanges();
    });

    // Force a fresh fetch from backend to get accurate server state
    this.cartSvc.viewCart(this.userId).subscribe({
      next:  () => { this.isLoading = false; this.cdr.detectChanges(); },
      error: () => { this.isLoading = false; this.hasError = true; this.cdr.detectChanges(); }
    });
  }

  ngOnDestroy(): void { this.sub?.unsubscribe(); }

  /* ── Cart actions ─────────────────────────────────────────── */

  add(item: CartItemDTO): void {
    this.cartSvc.addItem({
      userId:     this.userId,
      menuItemId: item.menuItem.itemId,
      quantity:   1
    });
  }

  reduce(item: CartItemDTO): void {
    this.cartSvc.reduceItem(this.userId, item.menuItem.itemId, item.orderItemId);
  }

   goBack(): void { history.back(); }
}