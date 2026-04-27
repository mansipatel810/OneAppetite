import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-stack" *ngIf="(svc.toasts$ | async) as list">
      <div *ngFor="let t of list"
           class="toast"
           [class.toast-success]="t.kind === 'success'"
           [class.toast-error]="t.kind === 'error'"
           [class.toast-info]="t.kind === 'info'"
           [class.toast-warning]="t.kind === 'warning'"
           (click)="svc.dismiss(t.id)">
        <i class="bi"
           [class.bi-check-circle-fill]="t.kind === 'success'"
           [class.bi-x-circle-fill]="t.kind === 'error'"
           [class.bi-info-circle-fill]="t.kind === 'info'"
           [class.bi-exclamation-triangle-fill]="t.kind === 'warning'"></i>
        <span>{{ t.message }}</span>
      </div>
    </div>
  `,
  styles: [`
    .toast-stack {
      position: fixed;
      top: 24px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 12px;
      pointer-events: none;
    }
    .toast {
      pointer-events: auto;
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 280px;
      max-width: 420px;
      padding: 14px 18px;
      border-radius: var(--oa-radius-lg);
      background: var(--oa-glass-strong);
      backdrop-filter: var(--oa-glass-blur);
      -webkit-backdrop-filter: var(--oa-glass-blur);
      color: var(--oa-text);
      border: 1px solid var(--oa-border);
      box-shadow: var(--oa-shadow-lg);
      border-left: 4px solid var(--oa-slate-400);
      font-size: 14px;
      font-weight: 500;
      font-family: var(--oa-font-sans);
      animation: toast-in 220ms var(--oa-ease-out);
      cursor: pointer;
      transition: transform var(--oa-dur-med) var(--oa-ease),
                  box-shadow var(--oa-dur-med) var(--oa-ease);
    }
    .toast:hover {
      transform: translateY(-1px);
      box-shadow: var(--oa-shadow-xl);
    }
    .toast i { font-size: 18px; }

    /* Brand-aligned semantic colors */
    .toast-success   { border-left-color: var(--oa-success); }
    .toast-success i { color: var(--oa-success); }
    .toast-error     { border-left-color: var(--oa-danger); }
    .toast-error i   { color: var(--oa-danger); }
    .toast-info      { border-left-color: var(--oa-primary); }
    .toast-info i    { color: var(--oa-primary); }
    .toast-warning   { border-left-color: var(--oa-warning); }
    .toast-warning i { color: var(--oa-warning); }

    @keyframes toast-in {
      from { transform: translateX(20px); opacity: 0; }
      to   { transform: translateX(0);   opacity: 1; }
    }
    @media (max-width: 480px) {
      .toast-stack { left: 16px; right: 16px; top: 16px; }
      .toast       { min-width: 0; }
    }
  `]
})
export class ToastHostComponent {
  svc = inject(ToastService);
}
