import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

/* ── Shared cart types ───────────────────────────────────────── */

export interface CartItemDTO {
  orderItemId: number;
  quantity: number;
  price: number;
  menuItem: {
    itemId: number;
    itemName: string;
    price: number;
    category?: string;
    isInStock?: boolean;
    dietaryType?: string;
    imageUrl?: string;
    minPrepTime?: number;
  };
}

export interface CartResponseDTO {
  orderId: number;
  tokenNumber?: string;
  status: string;
  totalAmount: number;
  orderTime?: string;
  readyTime?: string;
  vendor?: {
    userId?: number;
    name?: string;
    vendorName?: string;
    email?: string;
    phone?: string;
  };
  items?: CartItemDTO[];
  orderItems?: CartItemDTO[];
}

export interface WalletResponse {
  userId: number;
  walletBalance: number;
}

export interface CartRequest {
  userId: number;
  menuItemId: number;
  quantity: number;
}

/**
 * Multi-vendor cart state. We keep one bucket per vendor so the UI can render
 * grouped sections, while still exposing a flat `itemMap` of every item the
 * user is holding (so the menu page's per-item stepper continues to work).
 */
export interface VendorCartBucket {
  vendorId: number;
  vendorName: string;
  orderId: number;
  totalAmount: number;
  readyTime: string | null;
  status: string;
  items: CartItemDTO[];
}

export interface CartState {
  /** itemId → CartItemDTO  (flat lookup for menu-page steppers) */
  itemMap: Map<number, CartItemDTO>;
  /** One bucket per vendor; ordered by vendor name. */
  buckets: VendorCartBucket[];
  /** Sum across every bucket. */
  totalAmount: number;
}

const EMPTY_STATE: CartState = {
  itemMap:     new Map(),
  buckets:     [],
  totalAmount: 0,
};

@Injectable({ providedIn: 'root' })
export class CartService {
  private http       = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private isBrowser  = isPlatformBrowser(this.platformId);

  private readonly BASE = environment.apiBase;

  private _cart$ = new BehaviorSubject<CartState>({ ...EMPTY_STATE, itemMap: new Map() });
  readonly cart$ = this._cart$.asObservable();

  get snapshot(): CartState { return this._cart$.getValue(); }

  /* ── Computed helpers used by sidebar / menu page ─────────── */
  get cartCount(): number {
    let n = 0;
    this.snapshot.itemMap.forEach(ci => n += ci.quantity);
    return n;
  }
  get cartTotal(): number { return this.snapshot.totalAmount; }
  qtyOf(itemId: number): number {
    return this.snapshot.itemMap.get(itemId)?.quantity ?? 0;
  }

  /* ── API: load every cart bucket from backend ─────────────── */
  loadCart(userId: number): void {
    if (!this.isBrowser) return;
    this.http.get<CartResponseDTO[]>(`${this.BASE}/api/cart/view-all/${userId}`).pipe(
      catchError(() => of([] as CartResponseDTO[]))
    ).subscribe(list => this._applyAll(list));
  }

  /** Direct observable used by cart-view component on init. */
  viewAllCarts(userId: number): Observable<CartResponseDTO[]> {
    return this.http.get<CartResponseDTO[]>(`${this.BASE}/api/cart/view-all/${userId}`).pipe(
      tap(list => this._applyAll(list)),
      catchError(() => { this._reset(); return of([] as CartResponseDTO[]); })
    );
  }

  /* ── Add / Reduce ─────────────────────────────────────────── */
  addItem(payload: CartRequest): void {
    this._optimisticAdd(payload.menuItemId);
    this.http.post<CartItemDTO>(`${this.BASE}/api/cart/add`, payload).subscribe({
      next: () => this.loadCart(payload.userId),    // re-sync for accurate buckets
      error: () => this.loadCart(payload.userId),   // rollback
    });
  }

  reduceItem(userId: number, itemId: number, orderItemId: number): void {
    this._optimisticReduce(itemId);
    this.http.post<void>(`${this.BASE}/api/cart/reduce/${orderItemId}`, {}).subscribe({
      next:  () => this.loadCart(userId),
      error: () => this.loadCart(userId),
    });
  }

  /* ── Place every cart at once (multi-vendor split-order) ──── */
  placeAll(userId: number): Observable<CartResponseDTO[]> {
    return this.http.post<CartResponseDTO[]>(`${this.BASE}/api/cart/place-all/${userId}`, {})
      .pipe(tap(() => this._reset()));
  }

  getWalletBalance(userId: number): Observable<WalletResponse> {
    return this.http.get<WalletResponse>(`${this.BASE}/api/wallet/${userId}`);
  }

  clearCart(): void { this._reset(); }

  /* ── Private state helpers ────────────────────────────────── */
  private _applyAll(list: CartResponseDTO[]): void {
    const buckets: VendorCartBucket[] = [];
    const itemMap = new Map<number, CartItemDTO>();
    let total = 0;

    for (const dto of list ?? []) {
      const items = dto.items ?? dto.orderItems ?? [];
      const t = dto.totalAmount ?? 0;
      total += t;
      for (const ci of items) itemMap.set(ci.menuItem.itemId, ci);
      buckets.push({
        vendorId:    dto.vendor?.userId ?? 0,
        vendorName:  dto.vendor?.vendorName || dto.vendor?.name || 'Vendor',
        orderId:     dto.orderId,
        totalAmount: t,
        readyTime:   dto.readyTime ?? null,
        status:      dto.status ?? 'CART',
        items,
      });
    }
    buckets.sort((a, b) => a.vendorName.localeCompare(b.vendorName));

    this._cart$.next({ itemMap, buckets, totalAmount: total });
  }

  private _optimisticAdd(itemId: number): void {
    const state  = this.snapshot;
    const newMap = new Map(state.itemMap);
    const existing = newMap.get(itemId);
    if (existing) {
      newMap.set(itemId, { ...existing, quantity: existing.quantity + 1 });
    } else {
      newMap.set(itemId, {
        orderItemId: -1,
        quantity: 1,
        price: 0,
        menuItem: { itemId, itemName: '', price: 0 },
      });
    }
    this._cart$.next({ ...state, itemMap: newMap });
  }

  private _optimisticReduce(itemId: number): void {
    const state  = this.snapshot;
    const newMap = new Map(state.itemMap);
    const existing = newMap.get(itemId);
    if (!existing) return;
    if (existing.quantity > 1) {
      newMap.set(itemId, { ...existing, quantity: existing.quantity - 1 });
    } else {
      newMap.delete(itemId);
    }
    this._cart$.next({ ...state, itemMap: newMap });
  }

  private _reset(): void {
    this._cart$.next({ ...EMPTY_STATE, itemMap: new Map(), buckets: [] });
  }
}
