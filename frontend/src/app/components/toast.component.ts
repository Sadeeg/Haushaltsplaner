import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class]="toast.type" (click)="toastService.dismiss(toast.id)">
          <span class="toast-icon">
            @if (toast.type === 'success') {
              ✓
            } @else if (toast.type === 'error') {
              ✕
            } @else {
              ℹ
            }
          </span>
          <span class="toast-message">{{ toast.message }}</span>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 16px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 90%;
      width: 400px;
    }
    
    .toast {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 20px;
      border-radius: 10px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      cursor: pointer;
      animation: slideIn 0.3s ease;
      font-size: 0.95rem;
    }
    
    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    
    .toast.success {
      background: #4caf50;
      color: white;
    }
    
    .toast.error {
      background: #f44336;
      color: white;
    }
    
    .toast.info {
      background: #2196f3;
      color: white;
    }
    
    .toast-icon {
      font-size: 1.2rem;
      font-weight: bold;
    }
    
    .toast-message {
      flex: 1;
    }
  `]
})
export class ToastComponent {
  toastService = inject(ToastService);
}
