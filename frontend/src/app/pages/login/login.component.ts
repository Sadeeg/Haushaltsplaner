import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <h1>🏠 Haushaltsplaner</h1>
        <p>Anmeldung mit Nextcloud</p>
        
        @if (isAuthenticated) {
          <div class="welcome">
            <p>Willkommen, <strong>{{ username }}</strong>!</p>
            <button class="btn-primary" (click)="goHome()">
              Zur App →
            </button>
          </div>
        } @else {
          <button class="btn-nextcloud" (click)="login()">
            <span class="icon">☁️</span>
            Mit Nextcloud anmelden
          </button>
        }
        
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
    
    .welcome {
      p {
        margin-bottom: 16px;
        font-size: 1.1rem;
      }
    }
    
    .btn-nextcloud, .btn-primary {
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
    
    .btn-primary {
      background: #2e7d32;
      
      &:hover {
        background: #1b5e20;
      }
    }
    
    .info {
      margin-top: 24px;
      font-size: 0.85rem;
      color: #999;
    }
  `]
})
export class LoginComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  
  isAuthenticated = false;
  username = '';

  ngOnInit() {
    this.isAuthenticated = this.authService.isAuthenticated;
    this.username = this.authService.username;
  }

  login() {
    this.authService.login();
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
