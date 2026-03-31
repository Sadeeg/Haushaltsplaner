import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <h1>🏠 Haushaltsplaner</h1>
        <p>Anmeldung mit Nextcloud</p>
        
        <button class="btn-nextcloud" (click)="loginWithNextcloud()">
          <span class="icon">☁️</span>
          Mit Nextcloud anmelden
        </button>
        
        <p class="info">
          Für die Nutzung wird ein Nextcloud Account benötigt.
        </p>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);
    }
    
    .login-card {
      background: white;
      padding: 40px;
      border-radius: 16px;
      text-align: center;
      max-width: 400px;
      width: 100%;
      box-shadow: 0 10px 40px rgba(0,0,0,0.2);
      
      h1 {
        font-size: 1.5rem;
        margin-bottom: 8px;
        color: #333;
      }
      
      p {
        color: #666;
        margin-bottom: 24px;
      }
    }
    
    .btn-nextcloud {
      width: 100%;
      padding: 16px 24px;
      background: #0082c2;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 500;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      transition: background 0.2s;
      
      &:hover {
        background: #006ba3;
      }
      
      .icon {
        font-size: 1.5rem;
      }
    }
    
    .info {
      margin-top: 24px;
      font-size: 0.85rem;
      color: #999;
    }
  `]
})
export class LoginComponent {
  loginWithNextcloud() {
    // OAuth flow will be implemented
    window.location.href = '/api/oauth2/authorization/nextcloud';
  }
}
