import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { PLATFORM_ID, Inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { OrderService, VendorOrder } from '../services/order.service';

@Component({
  selector: 'app-vendor-kanban',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './vendor-kanban.component.html',
})
export class VendorKanbanComponent implements OnInit, OnDestroy {
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
  private pollingInterval: any;
  private knownOrderIds = new Set<number>(); // tracks orders already seen
  private audio: HTMLAudioElement | null = null;

  constructor(
    private authService: AuthService,
    private orderService: OrderService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.audio = new Audio('/orderPlaced.mp3');
    const session = this.authService.getSession();
    if (!session || session.role !== 'VENDOR') {
      this.router.navigate(['/login']);
      return;
    }
    this.vendorId = session.userId;
    this.vendorName = session.name;

    this.loadOrders();

    this.pollingInterval = setInterval(() => {
      this.loadOrders();
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
  }

  loadOrders(): void {
  this.orderService.getVendorOrders(this.vendorId).subscribe({
    next: (orders) => {
      // Only trigger sound for brand new PLACED orders
      const newPlacedOrders = orders.filter(
        o => o.status === 'PLACED' && !this.knownOrderIds.has(o.orderId)
      );

      // Play sound only if it's not the first load and there are new PLACED orders
      if (newPlacedOrders.length > 0 && this.knownOrderIds.size > 0) {
        this.playNotification();
      }

      // Track ALL order IDs regardless of status
      orders.forEach(o => this.knownOrderIds.add(o.orderId));

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

  playNotification(): void {
    if(!this.audio) return;
    this.audio.currentTime = 0; // rewind in case it's still playing
    this.audio.play().catch(err => {
      console.log('Audio play failed:', err);
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
    clearInterval(this.pollingInterval);
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }
}