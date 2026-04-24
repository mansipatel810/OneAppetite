import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';

/* ── Shared cart types (imported by menu + cart-view) ─────────── */

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
  orderItems?: CartItemDTO[];  // fallback key some backends use
}

export interface WalletResponse{
  userId: number;
  walletBalance: number;
}

export interface CartRequest {
  userId: number;
  menuItemId: number;
  quantity: number;
}

/* ── Local optimistic state ────────────────────────────────────── */
export interface CartState {
  /** itemId → CartItemDTO  (source of truth for steppers) */
  itemMap: Map<number, CartItemDTO>;
  totalAmount: number;
  orderId: number | null;
  vendor: CartResponseDTO['vendor'] | null;
  readyTime: string | null;
  tokenNumber: string | null;
  status: string;
}

const EMPTY_STATE: CartState = {
  itemMap:     new Map(),
  totalAmount: 0,
  orderId:     null,
  vendor:      null,
  readyTime:   null,
  tokenNumber: null,
  status:      'CART',
};

@Injectable({ providedIn: 'root' })
export class CartService {
  private http       = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private isBrowser  = isPlatformBrowser(this.platformId);

  private readonly BASE = 'http://localhost:8081';

  /* ── Observable state ──────────────────────────────────────── */
  private _cart$ = new BehaviorSubject<CartState>({ ...EMPTY_STATE, itemMap: new Map() });

  /** Subscribe to this in any component for live cart state */
  readonly cart$ = this._cart$.asObservable();

  get snapshot(): CartState { return this._cart$.getValue(); }

  /* ── Computed helpers ──────────────────────────────────────── */
  get cartCount(): number {
    let n = 0;
    this.snapshot.itemMap.forEach(ci => n += ci.quantity);
    return n;
  }

  get cartTotal(): number {
    let s = 0;
    this.snapshot.itemMap.forEach(ci => s += ci.menuItem.price * ci.quantity);
    return s;
  }

  qtyOf(itemId: number): number {
    return this.snapshot.itemMap.get(itemId)?.quantity ?? 0;
  }

  /* ── API: load cart from backend ───────────────────────────── */
  loadCart(userId: number): void {
    if (!this.isBrowser) return;
    this.http.get<CartResponseDTO>(`${this.BASE}/api/cart/view/${userId}`)
      .subscribe({
        next:  (res) => this._applyResponse(res),
        error: ()    => this._reset(),   // 404 = empty cart, that's fine
      });
  }

  /* ── API: add 1 unit ───────────────────────────────────────── */
  addItem(payload: CartRequest): void {
    // 1. Optimistic update — instant UI response
    this._optimisticAdd(payload.menuItemId);

    // 2. Background API call
    this.http.post<CartItemDTO>(`${this.BASE}/api/cart/add`, payload)
      .subscribe({
        next: (dto) => {
          // Authoritative update from server (fixes orderItemId, price, qty)
          const state   = this.snapshot;
          const newMap  = new Map(state.itemMap);
          newMap.set(dto.menuItem.itemId, dto);
          this._emit({
            ...state,
            itemMap:     newMap,
            totalAmount: this._sumMap(newMap),
          });
        },
        error: () => {
          // Rollback: reload from server
          this.loadCart(payload.userId);
        }
      });
  }

  /* ── API: reduce 1 unit ────────────────────────────────────── */
  reduceItem(userId: number, itemId: number, orderItemId: number): void {
    // 1. Optimistic update
    this._optimisticReduce(itemId);

    // 2. Background API call
    this.http.post<void>(`${this.BASE}/api/cart/reduce/${orderItemId}`, {})
      .subscribe({
        next: () => {
          // Light re-sync to get accurate server state (price, totals)
          this.loadCart(userId);
        },
        error: () => {
          // Rollback
          this.loadCart(userId);
        }
      });
  }

  /* ── Direct API observable (for cart-view page) ─────────────── */
  viewCart(userId: number): Observable<CartResponseDTO> {
    return this.http.get<CartResponseDTO>(`${this.BASE}/api/cart/view/${userId}`)
      .pipe(tap(res => this._applyResponse(res)));
  }

  /* ── API: place order (deducts wallet, status CART→PLACED) ──── */
  placeOrder(userId: number): Observable<CartResponseDTO> {
    return this.http.post<CartResponseDTO>(`${this.BASE}/api/cart/place/${userId}`, {})
      .pipe(tap(res => this._applyResponse(res)));
  }

  getWalletBalance(userId: number): Observable<WalletResponse> {
    return this.http.get<WalletResponse>(`${this.BASE}/api/wallet/${userId}`);
  }

  /* ── Reset cart state (after successful order) ───────────────── */
  clearCart(): void {
    this._reset();
  }

  /* ── Private helpers ─────────────────────────────────────────── */

  private _optimisticAdd(itemId: number): void {
    const state  = this.snapshot;
    const newMap = new Map(state.itemMap);
    const existing = newMap.get(itemId);

    if (existing) {
      newMap.set(itemId, { ...existing, quantity: existing.quantity + 1 });
    } else {
      // Placeholder entry — server response will fill in orderItemId
      newMap.set(itemId, {
        orderItemId: -1,   // unknown until server responds
        quantity:    1,
        price:       0,
        menuItem:    { itemId, itemName: '', price: 0 },
      });
    }

    this._emit({ ...state, itemMap: newMap, totalAmount: this._sumMap(newMap) });
  }

  private _optimisticReduce(itemId: number): void {
    const state    = this.snapshot;
    const newMap   = new Map(state.itemMap);
    const existing = newMap.get(itemId);

    if (!existing) return;

    if (existing.quantity > 1) {
      newMap.set(itemId, { ...existing, quantity: existing.quantity - 1 });
    } else {
      newMap.delete(itemId);
    }

    this._emit({ ...state, itemMap: newMap, totalAmount: this._sumMap(newMap) });
  }

  private _applyResponse(res: CartResponseDTO): void {
    const items  = res.items ?? res.orderItems ?? [];
    const newMap = new Map<number, CartItemDTO>();
    items.forEach(ci => newMap.set(ci.menuItem.itemId, ci));

    this._emit({
      itemMap:     newMap,
      totalAmount: res.totalAmount,
      orderId:     res.orderId,
      vendor:      res.vendor ?? null,
      readyTime:   res.readyTime ?? null,
      tokenNumber: res.tokenNumber ?? null,
      status:      res.status,
    });
  }

  private _reset(): void {
    this._emit({ ...EMPTY_STATE, itemMap: new Map() });
  }

  private _emit(state: CartState): void {
    this._cart$.next(state);
  }

  private _sumMap(map: Map<number, CartItemDTO>): number {
    let s = 0;
    map.forEach(ci => s += ci.menuItem.price * ci.quantity);
    return s;
  }
}