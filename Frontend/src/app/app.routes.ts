import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { VendorKanbanComponent } from './vendor-kanban/vendor-kanban.component';
import { ShellComponent }      from './shell/shell.component';
import { MenuComponent }       from './menu/menu.component';
import { CartViewComponent }   from './cart-view/cart-view.component';
import { VendorMenuComponent } from './vendor-menu-view/vendor-menu.component';
import { AdminUsersComponent } from './admin/admin-users/admin-users.component';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: 'admin/dashboard', component: AdminUsersComponent },
  { path: 'vendor/dashboard', component: VendorKanbanComponent },

  // ── Public (no sidebar/navbar) ────────────────────────────────
  { path: 'login',    component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'vendor/menu', component: VendorMenuComponent },
  

  // Admin-only user management screen (US-01 + US-02)
  // {
  //   path: 'admin/users',
  //   component: AdminUsersComponent,
  //   canActivate: [adminGuard],
  // },

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