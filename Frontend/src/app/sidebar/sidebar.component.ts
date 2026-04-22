import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService, LoginResponse } from '../services/auth.service';
import { LayoutService } from '../services/layout.service';
import { CartService, CartState, CartItemDTO } from '../services/cart.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit, OnDestroy {
  private authService   = inject(AuthService);
  private router        = inject(Router);
  private cdr           = inject(ChangeDetectorRef);
  private layoutService = inject(LayoutService);
  private cartSvc       = inject(CartService);

  user: LoginResponse | null = null;
  mobileMenuOpen = false;
  cartState!: CartState;

  private subs: Subscription[] = [];

  get cartItems(): CartItemDTO[] {
    const arr: CartItemDTO[] = [];
    this.cartState?.itemMap?.forEach(ci => arr.push(ci));
    return arr;
  }

  get cartTotal(): number {
    return this.cartState?.totalAmount ?? 0;
  }

  get cartCount(): number {
    let n = 0;
    this.cartState?.itemMap?.forEach(ci => n += ci.quantity);
    return n;
  }

  ngOnInit(): void {
    this.user = this.authService.getSession();

    // Subscribe to hamburger toggle from navbar
    this.subs.push(
      this.layoutService.toggle$.subscribe(() => {
        this.mobileMenuOpen = !this.mobileMenuOpen;
        this.cdr.detectChanges();
      })
    );

    // Subscribe to global cart state (BehaviorSubject — always live)
    this.subs.push(
      this.cartSvc.cart$.subscribe(state => {
        this.cartState = state;
        this.cdr.detectChanges();
      })
    );

    // Load cart from backend on init so it persists across page refreshes
    const userId = this.user?.userId;
    if (userId) {
      this.cartSvc.loadCart(userId);
    }
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  close(): void {
    this.mobileMenuOpen = false;
    this.cdr.detectChanges();
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }

  goToCart(): void {
    this.close();
    this.router.navigate(['/cart']);
  }
}