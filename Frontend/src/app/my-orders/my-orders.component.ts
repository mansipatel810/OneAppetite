import {
  Component, OnInit, OnDestroy, ChangeDetectorRef,
  inject, afterNextRender, HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { OrderHistoryService } from '../services/order-history.service';
import { CartResponseDTO, CartItemDTO } from '../services/cart.service';
import { ToastService } from '../services/toast.service';

type StatusFilter = 'ALL' | 'PLACED' | 'PREPARING' | 'READY' | 'PICKED_UP';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.css']
})
export class MyOrdersComponent implements OnInit, OnDestroy {
  private auth   = inject(AuthService);
  private orders = inject(OrderHistoryService);
  private router = inject(Router);
  private toast  = inject(ToastService);
  private cdr    = inject(ChangeDetectorRef);

  isLoading = true;
  hasError  = false;
  all: CartResponseDTO[] = [];
  filter: StatusFilter = 'ALL';

  /** poll handle, set after the first browser-side init */
  private pollHandle: any;
  /** cached userId so the poll closure doesn't repeatedly read session */
  private userId: number | null = null;
  /** snapshot of last seen statuses keyed by orderId, used to fire toasts */
  private lastStatuses = new Map<number, string>();

  readonly filters: { key: StatusFilter; label: string }[] = [
    { key: 'ALL',       label: 'All' },
    { key: 'PLACED',    label: 'Placed' },
    { key: 'PREPARING', label: 'Preparing' },
    { key: 'READY',     label: 'Ready' },
    { key: 'PICKED_UP', label: 'Picked Up' },
  ];

  constructor() {
    // Browser-only — Angular SSR runs ngOnInit on the server, where
    // localStorage is empty, which would redirect us to /login on every refresh.
    afterNextRender(() => {
      const session = this.auth.getSession();
      if (!session?.userId) {
        this.router.navigate(['/login']);
        return;
      }
      this.userId = session.userId;
      this.load(session.userId, /*silent*/ false);

      // Auto-refresh every 10s so order status transitions surface without
      // requiring a manual reload.
      this.pollHandle = setInterval(() => {
        if (this.userId) this.load(this.userId, /*silent*/ true);
      }, 10_000);
    });
  }

  ngOnInit(): void { /* session-bound work moved to afterNextRender */ }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  /** When the tab regains focus, fetch immediately for an "instant" feel. */
  @HostListener('window:focus')
  onWindowFocus(): void {
    if (this.userId) this.load(this.userId, /*silent*/ true);
  }
  @HostListener('document:visibilitychange')
  onVisibilityChange(): void {
    if (document.visibilityState === 'visible' && this.userId) {
      this.load(this.userId, /*silent*/ true);
    }
  }

  setFilter(f: StatusFilter): void { this.filter = f; }

  get filtered(): CartResponseDTO[] {
    if (this.filter === 'ALL') return this.all;
    return this.all.filter(o => String(o.status) === this.filter);
  }

  itemsOf(order: CartResponseDTO): CartItemDTO[] {
    return order.items ?? order.orderItems ?? [];
  }

  totalQty(order: CartResponseDTO): number {
    return this.itemsOf(order).reduce((n, i) => n + i.quantity, 0);
  }

  vendorLabel(order: CartResponseDTO): string {
    const v = order.vendor;
    return v?.vendorName || v?.name || 'Vendor';
  }

  formatTime(iso?: string | null): string {
    if (!iso) return '—';
    try {
      const d = new Date(iso);
      return d.toLocaleString([], {
        month: 'short', day: '2-digit',
        hour: '2-digit', minute: '2-digit',
      });
    } catch { return iso; }
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'PLACED':    return 'badge-placed';
      case 'PREPARING': return 'badge-preparing';
      case 'READY':     return 'badge-ready';
      case 'PICKED_UP':
      case 'COMPLETED': return 'badge-completed';
      default:          return 'badge-neutral';
    }
  }

  

  /**
   * @param silent  when true, do not flip the loading spinner / error state.
   *                Used by the 10s poll so the page doesn't flicker.
   */
  private load(userId: number, silent: boolean): void {
    if (!silent) {
      this.isLoading = true;
      this.hasError  = false;
    }
    this.orders.getHistory(userId).subscribe({
      next: (list) => {
        const sorted = (list ?? []).slice().sort((a, b) => {
          const ta = a.orderTime ? new Date(a.orderTime).getTime() : 0;
          const tb = b.orderTime ? new Date(b.orderTime).getTime() : 0;
          return tb - ta;
        });

        // Detect status transitions vs the previous snapshot and toast them.
        if (silent && this.lastStatuses.size > 0) {
          for (const order of sorted) {
            const prev = this.lastStatuses.get(order.orderId);
            const curr = String(order.status);
            if (prev && prev !== curr) {
              const token = order.tokenNumber ?? `#${order.orderId}`;
              if (curr === 'PREPARING') this.toast.info(`Order ${token} is now being prepared`);
              else if (curr === 'READY') this.toast.success(`Order ${token} is ready for pickup!`);
              else if (curr === 'PICKED_UP') this.toast.success(`Order ${token} — picked up. Enjoy!`);
              else if (curr === 'COMPLETED') this.toast.success(`Order ${token} completed`);
            }
          }
        }
        // Refresh the snapshot
        this.lastStatuses.clear();
        for (const o of sorted) this.lastStatuses.set(o.orderId, String(o.status));

        this.all = sorted;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        if (!silent) {
          this.hasError  = true;
          this.isLoading = false;
          this.toast.error('Could not load order history');
        }
        this.cdr.detectChanges();
      }
    });
  }
}
