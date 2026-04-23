import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AdminUsersComponent } from './admin/admin-users/admin-users.component';
import { VendorMenuComponent } from './vendor-menu/vendor-menu.component';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // All three role-based paths point to the same dashboard for now.
  // Replace with role-specific components when ready.
  { path: 'dashboard', component: DashboardComponent },
  { path: 'admin/dashboard', component: DashboardComponent },
  { path: 'vendor/dashboard', component: DashboardComponent },

  // View a single vendor's menu (linked from the dashboard vendor grid)
  { path: 'vendor/:id', component: VendorMenuComponent },

  // Admin-only user management screen (US-01 + US-02)
  {
    path: 'admin/users',
    component: AdminUsersComponent,
    canActivate: [adminGuard],
  },

  // Default redirect
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Catch-all → login
  { path: '**', redirectTo: 'login' },
];