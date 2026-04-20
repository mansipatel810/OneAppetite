import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { PLATFORM_ID, Inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { OrderService, VendorOrder } from '../services/order.service';

@Component({
  selector: 'app-vendor-kanban',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './vendor-kanban.component.html',
})
export class VendorKanbanComponent implements OnInit {
  columns: { label: string; status: string }[] = [
    { label: 'Placed',    status: 'PLACED' },
    { label: 'Preparing', status: 'PREPARING' },
    { label: 'Ready',     status: 'READY' },
  ];

  ordersByStatus: Record<string, VendorOrder[]> = {};
  vendorId!: number;
  vendorName = '';
  loading = true;
  error = '';

  constructor(
    private authService: AuthService,
    private orderService: OrderService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const session = this.authService.getSession();
    if (!session || session.role !== 'VENDOR') {
      this.router.navigate(['/login']);
      return;
    }
    this.vendorId = session.userId;
    this.vendorName = session.name;
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.orderService.getVendorOrders(this.vendorId).subscribe({
      next: (orders) => {
        this.columns.forEach(col => {
          this.ordersByStatus[col.status] = orders.filter(o => o.status === col.status);
        });
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load orders.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  moveToNext(order: VendorOrder): void {
    const flow = ['PLACED', 'PREPARING', 'READY'];
    const nextStatus = flow[flow.indexOf(order.status) + 1];
    if (!nextStatus) return;

    this.orderService.updateOrderStatus(order.orderId, nextStatus).subscribe({
      next: () => this.loadOrders()
    });
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }
}