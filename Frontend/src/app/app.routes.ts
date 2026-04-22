import { Routes } from '@angular/router';
import { LoginComponent }      from './login/login.component';
import { RegisterComponent }   from './register/register.component';
import { ShellComponent }      from './shell/shell.component';
import { DashboardComponent }  from './dashboard/dashboard.component';
import { MenuComponent }       from './menu/menu.component';
import { CartViewComponent }   from './cart-view/cart-view.component';

export const routes: Routes = [

  // ── Public (no sidebar/navbar) ────────────────────────────────
  { path: 'login',    component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // ── Authenticated (inside ShellComponent) ─────────────────────
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: 'dashboard',        component: DashboardComponent },
      { path: 'admin/dashboard',  component: DashboardComponent },
      { path: 'vendor/dashboard', component: DashboardComponent },
      { path: 'vendor/:vendorId', component: MenuComponent },
      { path: 'cart',             component: CartViewComponent },
    ]
  },

  { path: '**', redirectTo: 'login' },
];