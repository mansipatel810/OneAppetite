import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // All three role-based paths point to the same dashboard for now.
  // Replace with role-specific components when ready.
  { path: 'dashboard', component: DashboardComponent },
  { path: 'admin/dashboard', component: DashboardComponent },
  { path: 'vendor/dashboard', component: DashboardComponent },

  // Default redirect
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Catch-all → login
  { path: '**', redirectTo: 'login' },
];