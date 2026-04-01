import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';
import { ToastComponent } from './components/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, ToastComponent],
  template: `
    <div class="app-container">
      <nav class="bottom-nav">
        <a routerLink="/" class="nav-item">
          <span class="nav-icon">🏠</span>
          <span class="nav-label">Heute</span>
        </a>
        <a routerLink="/tasks" class="nav-item">
          <span class="nav-icon">📋</span>
          <span class="nav-label">Aufgaben</span>
        </a>
        <a routerLink="/templates" class="nav-item">
          <span class="nav-icon">📝</span>
          <span class="nav-label">Vorlagen</span>
        </a>
        <a routerLink="/leaderboard" class="nav-item">
          <span class="nav-icon">🏆</span>
          <span class="nav-label">Punkte</span>
        </a>
        <a routerLink="/profile" class="nav-item">
          <span class="nav-icon">👤</span>
          <span class="nav-label">Profil</span>
        </a>
      </nav>
      <main class="main-content">
        <router-outlet></router-outlet>
      </main>
      <app-toast></app-toast>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    
    .main-content {
      flex: 1;
      padding: 16px;
      padding-bottom: 80px;
      max-width: 600px;
      margin: 0 auto;
      width: 100%;
    }
    
    .bottom-nav {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      background: white;
      display: flex;
      justify-content: space-around;
      padding: 8px 0;
      box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
      z-index: 100;
    }
    
    @media (prefers-color-scheme: dark) {
      .bottom-nav {
        background: #1e1e1e;
      }
    }
    
    .nav-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 8px 12px;
      color: #666;
      text-decoration: none;
      transition: color 0.2s;
      
      &.active {
        color: #1976d2;
      }
      
      &:hover {
        color: #1976d2;
      }
    }
    
    .nav-icon {
      font-size: 22px;
    }
    
    .nav-label {
      font-size: 11px;
      margin-top: 2px;
    }
  `]
})
export class AppComponent implements OnInit {
  private authService = inject(AuthService);

  ngOnInit() {
    // Initialize auth service from storage
    this.authService.initFromStorage();
  }
}
