import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { OrderHistoryService } from '../services/order-history.service';
import { CartResponseDTO, CartItemDTO } from '../services/cart.service';
import { ToastService } from '../services/toast.service';

type StatusFilter = 'ALL' | 'PLACED' | 'PREPARING' | 'READY' | 'COMPLETED';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.css']
})
export class MyOrdersComponent implements OnInit {
  private auth   = inject(AuthService);
  private orders = inject(OrderHistoryService);
  private router = inject(Router);
  private toast  = inject(ToastService);
  private cdr    = inject(ChangeDetectorRef);

  isLoading = true;
  hasError  = false;
  all: CartResponseDTO[] = [];
  filter: StatusFilter = 'ALL';

  readonly filters: { key: StatusFilter; label: string }[] = [
    { key: 'ALL',       label: 'All' },
    { key: 'PLACED',    label: 'Placed' },
    { key: 'PREPARING', label: 'Preparing' },
    { key: 'READY',     label: 'Ready' },
    { key: 'COMPLETED', label: 'Completed' },
  ];

  ngOnInit(): void {
    const session = this.auth.getSession();
    if (!session?.userId) {
      this.router.navigate(['/login']);
      return;
    }
    this.load(session.userId);
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
      case 'COMPLETED': return 'badge-completed';
      default:          return 'badge-neutral';
    }
  }

  reorder(): void {
    this.toast.info('Reorder coming soon — heading back to dashboard');
    this.router.navigate(['/dashboard']);
  }

  private load(userId: number): void {
    this.isLoading = true;
    this.hasError  = false;
    this.orders.getHistory(userId).subscribe({
      next: (list) => {
        this.all = (list ?? []).slice().sort((a, b) => {
          const ta = a.orderTime ? new Date(a.orderTime).getTime() : 0;
          const tb = b.orderTime ? new Date(b.orderTime).getTime() : 0;
          return tb - ta;
        });
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.hasError  = true;
        this.isLoading = false;
        this.toast.error('Could not load order history');
        this.cdr.detectChanges();
      }
    });
  }
}
