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
        <main class="page-container">
          <div class="page-inner">
            <router-outlet></router-outlet>
          </div>
        </main>
      </div>
    </div>
    <!-- Chatbot is now rendered globally from app.html so it appears on
         vendor and admin pages too (which don't use this shell). -->
  `,
  styles: [`
    .app-shell {
      display: flex;
      height: 100vh;
      height: 100dvh;
      overflow: hidden;
      background: var(--oa-bg);
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
      /* Page-level breathing room — generous gutter from sidebar / navbar */
      padding: var(--oa-page-pad-y) var(--oa-page-pad-x) calc(var(--oa-page-pad-y) + 16px);
    }
    .page-inner {
      max-width: var(--oa-page-max);
      margin: 0 auto;
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: var(--oa-section-gap);
    }
    @media (max-width: 1024px) {
      .page-container {
        padding: var(--oa-page-pad-y) var(--oa-page-pad-x-tablet);
      }
    }
    @media (max-width: 768px) {
      .main-content { padding-top: 0; }
      .page-container {
        padding: var(--oa-page-pad-y-mobile) var(--oa-page-pad-x-mobile);
      }
    }
  `]
})
export class ShellComponent {}
