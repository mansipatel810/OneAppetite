import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { VendorKanbanComponent } from './vendor-kanban/vendor-kanban.component';
import { ShellComponent }      from './shell/shell.component';
import { MenuComponent }       from './menu/menu.component';
import { CartViewComponent }   from './cart-view/cart-view.component';
import { AdminUsersComponent } from './admin/admin-users/admin-users.component';
import { MyOrdersComponent }   from './my-orders/my-orders.component';
import { SettingsComponent }   from './settings/settings.component';
import { VendorMenuComponent } from './vendor-menu/vendor-menu.component';
import { VendorSettingsComponent } from './vendor-settings/vendor-settings.component';
import { AdminSettingsComponent }  from './admin-settings/admin-settings.component';

export const routes: Routes = [
  { path: 'admin/dashboard',  component: AdminUsersComponent },
  { path: 'admin/settings',   component: AdminSettingsComponent },
  { path: 'vendor/dashboard', component: VendorKanbanComponent },
  { path: 'vendor/menu',      component: VendorMenuComponent },
  { path: 'vendor/settings',  component: VendorSettingsComponent },

  // ── Public (no sidebar/navbar) ────────────────────────────────
  { path: 'login',    component: LoginComponent },
  { path: 'register', component: RegisterComponent },

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
      { path: 'my-orders',        component: MyOrdersComponent },
      { path: 'settings',         component: SettingsComponent },
    ]
  },

  // Catch-all → login
  { path: '**', redirectTo: 'login' },
];
