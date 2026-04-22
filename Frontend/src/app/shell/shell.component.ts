import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { NavbarComponent }  from '../navbar/navbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, NavbarComponent],
  template: `
    <div class="app-shell">
      <app-sidebar></app-sidebar>
      <div class="main-content">
        <app-navbar></app-navbar>
        <div class="page-container">
          <router-outlet></router-outlet>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .app-shell {
      display: flex;
      height: 100vh;
      height: 100dvh;
      overflow: hidden;
      background: #f5f5f4;
    }
    .main-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      min-width: 0;
    }
    .page-container {
      flex: 1;
      overflow-y: auto;
      overflow-x: hidden;
      -webkit-overflow-scrolling: touch;
    }
    @media (max-width: 768px) {
      .main-content { padding-top: 56px; }
    }
  `]
})
export class ShellComponent {}