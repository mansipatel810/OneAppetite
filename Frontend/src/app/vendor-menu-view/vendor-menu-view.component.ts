import { Component, OnInit, ChangeDetectorRef, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { MenuService, MenuItem } from '../services/menu.service';

@Component({
  selector: 'app-vendor-menu-view',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './vendor-menu-view.component.html',
})
export class VendorMenuViewComponent implements OnInit {
  menuItems: MenuItem[] = [];
  vendorId!: number;
  vendorName = '';
  loading = true;
  error = '';

  constructor(
    private authService: AuthService,
    private menuService: MenuService,
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
    this.loadMenu();
  }

  loadMenu(): void {
    this.menuService.getMenuItems(this.vendorId).subscribe({
      next: (items) => {
        setTimeout(() => {
          this.menuItems = items;
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        setTimeout(() => {
          this.error = 'Failed to load menu items.';
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  toggleStock(item: MenuItem): void {
    this.menuService.toggleStock(this.vendorId, item.itemId).subscribe({
      next: (updated: MenuItem) => {
        setTimeout(() => {
          item.isInStock = updated.isInStock;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.error = 'Failed to update stock status.';
      }
    });
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }
}