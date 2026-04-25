import {
  Component, OnInit, OnDestroy,
  ChangeDetectorRef, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { CartService, CartState, CartItemDTO, WalletResponse } from '../services/cart.service';

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
  isLoading    = true;
  hasError     = false;

  isProcessing = false;
  orderSuccess = false;
  orderError   = '';
  confirmedToken = '';
  confirmedTotal = 0;
  walletBalance = 0;

  private sub?: Subscription;

  get items(): CartItemDTO[] {
    const arr: CartItemDTO[] = [];
    this.cartState?.itemMap.forEach(ci => arr.push(ci));
    return arr;
  }

  get isEmpty(): boolean {
    return !this.isLoading && !this.orderSuccess && (this.cartState?.itemMap.size ?? 0) === 0;
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
    if (!session || !session.userId) {
      // redirect if no valid session
      this.router.navigate(['/login']);
      return;
    }
    this.userId = session.userId;

    this.sub = this.cartSvc.cart$.subscribe(state => {
      this.cartState = state;
      this.isLoading = false;
      this.cdr.detectChanges();
    });

    this.cartSvc.viewCart(this.userId).subscribe({
      next:  () => { this.isLoading = false; this.cdr.detectChanges(); },
      error: () => { this.isLoading = false; this.hasError = true; this.cdr.detectChanges(); }
    });

    this.loadWalletBalance();
  }

  ngOnDestroy(): void { this.sub?.unsubscribe(); }

  private loadWalletBalance(): void {
    this.cartSvc.getWalletBalance(this.userId).subscribe({
      next: (res: WalletResponse) => {
        this.walletBalance = res.walletBalance;
        this.cdr.detectChanges();
      },
      error: () => console.error('Failed to load wallet balance')
    });
  }

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

  placeOrder(): void {
    if (this.isProcessing) return;

    // pre-check for balance
    if (this.walletBalance < (this.cartState?.totalAmount ?? 0)) {
      this.orderError = 'Insufficient wallet balance. Please top up and try again.';
      this.cdr.detectChanges();
      return;
    }

    this.isProcessing = true;
    this.orderError   = '';
    this.confirmedTotal = this.cartState?.totalAmount ?? 0;
    this.cdr.detectChanges();

    setTimeout(() => {
      this.cartSvc.placeOrder(this.userId).subscribe({
        next: (res) => {
          this.isProcessing    = false;
          this.orderSuccess    = true;
          this.confirmedToken  = res.tokenNumber ?? this.cartState?.tokenNumber ?? '';
          this.cartSvc.clearCart();

          // refresh wallet balance after debit
          this.loadWalletBalance();

          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isProcessing = false;
          const msg = err?.error?.message || err?.error || '';
          if (typeof msg === 'string' && msg.toLowerCase().includes('insufficient')) {
            this.orderError = 'Insufficient wallet balance. Please top up and try again.';
          } else {
            this.orderError = 'Payment failed. Please try again.';
          }
          this.cdr.detectChanges();
        }
      });
    }, 3000);
  }

  goBack(): void { history.back(); }
}
