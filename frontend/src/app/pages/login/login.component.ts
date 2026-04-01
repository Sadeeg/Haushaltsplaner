import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <h1>🏠 Haushaltsplaner</h1>
        <p>Anmeldung mit Nextcloud</p>
        
        <button class="btn-nextcloud" (click)="loginWithNextcloud()" [disabled]="loading">
          @if (loading) {
            <span class="spinner"></span>
            Wird weitergeleitet...
          } @else {
            <span class="icon">☁️</span>
            Mit Nextcloud anmelden
          }
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
      
      &:hover:not(:disabled) {
        background: #006ba3;
      }
      
      &:disabled {
        opacity: 0.7;
        cursor: not-allowed;
      }
      
      .icon {
        font-size: 1.5rem;
      }
      
      .spinner {
        width: 20px;
        height: 20px;
        border: 2px solid white;
        border-top-color: transparent;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
    }
    
    @keyframes spin {
      to {
        transform: rotate(360deg);
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
  private api = inject(ApiService);
  private router = inject(Router);
  
  loading = false;

  ngOnInit() {
    // Check if user is already logged in
    if (this.authService.isLoggedIn()) {
      this.redirectAfterLogin();
      return;
    }
    
    // Check if this is an OAuth callback with user data
    if (this.authService.handleOAuthCallback()) {
      this.redirectAfterLogin();
      return;
    }
    
    // Check for redirect URL and clear it if login was successful
    const redirectUrl = localStorage.getItem('redirectUrl');
    if (redirectUrl) {
      localStorage.removeItem('redirectUrl');
    }
  }

  loginWithNextcloud() {
    this.loading = true;
    // Redirect to OAuth endpoint
    window.location.href = '/api/oauth2/authorization/nextcloud';
  }

  private redirectAfterLogin() {
    // Check if there's a stored redirect URL
    const redirectUrl = localStorage.getItem('redirectUrl');
    if (redirectUrl) {
      localStorage.removeItem('redirectUrl');
      this.router.navigateByUrl(redirectUrl);
    } else {
      // Default redirect to home
      this.router.navigate(['/']);
    }
  }
}
