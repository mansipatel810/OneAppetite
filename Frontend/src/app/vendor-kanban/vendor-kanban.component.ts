import {
  Component, OnInit, OnDestroy, ChangeDetectorRef,
  PLATFORM_ID, Inject, inject
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import {
  CdkDragDrop, DragDropModule, transferArrayItem
} from '@angular/cdk/drag-drop';
import { AuthService } from '../services/auth.service';
import { OrderService, VendorOrder } from '../services/order.service';
import { ToastService } from '../services/toast.service';
import { ThemeService, Theme } from '../services/theme.service';
import { Subscription } from 'rxjs';

type KanbanStatus = 'PLACED' | 'PREPARING' | 'READY';

@Component({
  selector: 'app-vendor-kanban',
  standalone: true,
  imports: [CommonModule, RouterLink, DragDropModule],
  templateUrl: './vendor-kanban.component.html',
  styleUrls: ['./vendor-kanban.component.css'],
})
export class VendorKanbanComponent implements OnInit, OnDestroy {
  private toast    = inject(ToastService);
  private themeSvc = inject(ThemeService);

  theme: Theme = 'light';
  private themeSub?: Subscription;

  readonly columns: { label: string; status: KanbanStatus; listId: string }[] = [
    { label: 'Placed',    status: 'PLACED',    listId: 'list-PLACED' },
    { label: 'Preparing', status: 'PREPARING', listId: 'list-PREPARING' },
    { label: 'Ready',     status: 'READY',     listId: 'list-READY' },
  ];
  readonly connectedLists = this.columns.map(c => c.listId);

  ordersByStatus: Record<KanbanStatus, VendorOrder[]> = {
    PLACED: [], PREPARING: [], READY: [],
  };

  vendorId!: number;
  vendorName = '';
  loading = true;
  error = '';

  private pollingInterval: any;
  private knownOrderIds = new Set<number>();
  private audio: HTMLAudioElement | null = null;
  private inFlightStatusUpdates = new Set<number>();

  constructor(
    private authService: AuthService,
    private orderService: OrderService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    if (isPlatformBrowser(this.platformId)) {
      this.audio = new Audio('/orderPlaced.mp3');
    }
  }

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.themeSub = this.themeSvc.theme$.subscribe(t => {
      this.theme = t;
      this.cdr.detectChanges();
    });

    const session = this.authService.getSession();
    if (!session || session.role !== 'VENDOR') {
      this.router.navigate(['/login']);
      return;
    }
    this.vendorId   = session.userId;
    this.vendorName = session.name;

    this.loadOrders();
    this.pollingInterval = setInterval(() => this.loadOrders(), 5000);
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    this.themeSub?.unsubscribe();
  }

  toggleTheme(): void {
    this.themeSvc.toggle();
    this.toast.success(this.themeSvc.current === 'dark'
      ? 'Theme updated to Dark Mode'
      : 'Theme updated to Light Mode');
  }

  loadOrders(): void {
    this.orderService.getVendorOrders(this.vendorId).subscribe({
      next: (orders) => {
        const newPlaced = orders.filter(
          o => o.status === 'PLACED' && !this.knownOrderIds.has(o.orderId)
        );
        if (newPlaced.length > 0 && this.knownOrderIds.size > 0) {
          this.playNotification();
          this.toast.info(
            newPlaced.length === 1
              ? `New order from ${newPlaced[0].user.name}`
              : `${newPlaced.length} new orders received`
          );
        }
        orders.forEach(o => this.knownOrderIds.add(o.orderId));

        // Don't clobber columns where a drop is in flight (avoids snap-back)
        const next: Record<KanbanStatus, VendorOrder[]> = {
          PLACED:    [], PREPARING: [], READY: [],
        };
        for (const o of orders) {
          if ((['PLACED','PREPARING','READY'] as KanbanStatus[]).includes(o.status as KanbanStatus)) {
            next[o.status as KanbanStatus].push(o);
          }
        }
        this.ordersByStatus = next;

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
    if (!this.audio) return;
    this.audio.currentTime = 0;
    this.audio.play().catch(() => {/* autoplay blocked — silently ignore */});
  }

  /* ── Drag and drop ───────────────────────────────────────────── */
  onDrop(event: CdkDragDrop<VendorOrder[]>, target: KanbanStatus): void {
    if (event.previousContainer === event.container) return; // re-order in same column → noop

    const order = event.item.data as VendorOrder;
    const sourceStatus = order.status as KanbanStatus;

    if (!this.isValidTransition(sourceStatus, target)) {
      this.toast.warning(`Can't move ${order.tokenNumber} from ${sourceStatus} to ${target}`);
      return;
    }
    if (this.inFlightStatusUpdates.has(order.orderId)) return;

    // Optimistic move
    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex,
    );
    order.status = target;
    this.inFlightStatusUpdates.add(order.orderId);
    this.cdr.detectChanges();

    this.orderService.updateOrderStatus(order.orderId, target).subscribe({
      next: () => {
        this.inFlightStatusUpdates.delete(order.orderId);
        this.toast.success(
          target === 'PREPARING' ? `Order ${order.tokenNumber} moved to Preparing` :
          target === 'READY'     ? `Order ${order.tokenNumber} marked Ready` :
                                   `Order ${order.tokenNumber} updated`
        );
      },
      error: () => {
        // Rollback by reloading
        this.inFlightStatusUpdates.delete(order.orderId);
        this.toast.error(`Could not update order ${order.tokenNumber}. Reverting…`);
        this.loadOrders();
      }
    });
  }

  private isValidTransition(from: KanbanStatus, to: KanbanStatus): boolean {
    if (from === to) return false;
    // Forward only: PLACED→PREPARING→READY (skipping not allowed)
    const flow: KanbanStatus[] = ['PLACED', 'PREPARING', 'READY'];
    return flow.indexOf(to) === flow.indexOf(from) + 1;
  }

  /* ── Button fallback (still works alongside DnD) ─────────────── */
  moveToNext(order: VendorOrder): void {
    const flow: KanbanStatus[] = ['PLACED', 'PREPARING', 'READY'];
    const next = flow[flow.indexOf(order.status as KanbanStatus) + 1];
    if (!next) return;
    if (this.inFlightStatusUpdates.has(order.orderId)) return;

    this.inFlightStatusUpdates.add(order.orderId);
    this.orderService.updateOrderStatus(order.orderId, next).subscribe({
      next: () => {
        this.inFlightStatusUpdates.delete(order.orderId);
        this.toast.success(`Order ${order.tokenNumber} moved to ${next.charAt(0) + next.slice(1).toLowerCase()}`);
        this.loadOrders();
      },
      error: () => {
        this.inFlightStatusUpdates.delete(order.orderId);
        this.toast.error(`Could not update order ${order.tokenNumber}.`);
      }
    });
  }

  /* ── Mark Ready → Picked Up ──────────────────────────────── */
  markPickedUp(order: VendorOrder): void {
    if (order.status !== 'READY') return;
    if (this.inFlightStatusUpdates.has(order.orderId)) return;

    this.inFlightStatusUpdates.add(order.orderId);
    this.orderService.updateOrderStatus(order.orderId, 'PICKED_UP').subscribe({
      next: () => {
        this.inFlightStatusUpdates.delete(order.orderId);
        this.toast.success(`Order ${order.tokenNumber} marked as picked up`);
        // Remove from the Ready column locally for instant feedback;
        // the next poll will reconcile from the server.
        this.ordersByStatus = {
          ...this.ordersByStatus,
          READY: this.ordersByStatus.READY.filter(o => o.orderId !== order.orderId),
        };
        this.cdr.detectChanges();
        this.loadOrders();
      },
      error: () => {
        this.inFlightStatusUpdates.delete(order.orderId);
        this.toast.error(`Could not update order ${order.tokenNumber}.`);
      }
    });
  }

  logout(): void {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }
}
