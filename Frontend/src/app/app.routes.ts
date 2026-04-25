import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { VendorKanbanComponent } from './vendor-kanban/vendor-kanban.component';
import { ShellComponent }      from './shell/shell.component';
import { MenuComponent }       from './menu/menu.component';
import { CartViewComponent }   from './cart-view/cart-view.component';
import { AdminUsersComponent } from './admin/admin-users/admin-users.component';
import { AdminDashboardComponent } from './admin/admin-dashboard/admin-dashboard.component';
import { AdminVendorMenuComponent } from './admin/admin-vendor-menu/admin-vendor-menu.component';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: 'admin/dashboard',
    component: AdminDashboardComponent,
    canActivate: [adminGuard],
  },
  { path: 'vendor/dashboard', component: VendorKanbanComponent },

  // ── Public (no sidebar/navbar) ────────────────────────────────
  { path: 'login',    component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // Admin-only user management screen (US-01 + US-02)
  // {
  //   path: 'admin/users',
  //   component: AdminUsersComponent,
  //   canActivate: [adminGuard],
  // },

  // Admin-only read-only view of any vendor's menu
  {
    path: 'admin/vendors/:vendorId/menu',
    component: AdminVendorMenuComponent,
    canActivate: [adminGuard],
  },

  // Default redirect: root → login
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // ── Authenticated (inside ShellComponent) ─────────────────────
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: 'dashboard',        component: DashboardComponent },
      { path: 'vendor/:vendorId', component: MenuComponent },
      { path: 'cart',             component: CartViewComponent },
    ]
  },

  // Catch-all → login
  { path: '**', redirectTo: 'login' },
];