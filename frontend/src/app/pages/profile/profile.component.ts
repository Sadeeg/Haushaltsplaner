import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { User } from '../../models/task.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="profile-page">
      <header class="header">
        <h1>👤 Profil</h1>
      </header>

      @if (loading) {
        <div class="loading-spinner">
          <div class="spinner"></div>
        </div>
      } @else if (user) {
        <div class="profile-card">
          <div class="avatar">
            {{ user.displayName?.charAt(0) || user.username?.charAt(0) || '?' }}
          </div>
          <h2>{{ user.displayName || user.username }}</h2>
          <p class="email">{{ user.email }}</p>
        </div>

        <div class="section">
          <h3>Telegram Verknüpfung</h3>
          @if (user.hasTelegram) {
            <div class="status connected">
              <span>✓</span> Telegram verbunden
            </div>
          } @else {
            <div class="telegram-setup">
              <p>Verknüpfe deinen Telegram Account, um Benachrichtigungen zu erhalten.</p>
              <ol>
                <li>Öffne Telegram und suche nach <strong>&#64;HaushaltsplanerBot</strong></li>
                <li>Sende <strong>/start</strong></li>
                <li>Gib den erhaltenen Code hier ein:</li>
              </ol>
              <div class="code-input">
                <input type="text" [(ngModel)]="verificationCode" placeholder="ABC-123-XYZ" maxlength="11">
                <button class="btn-primary" (click)="linkTelegram()" [disabled]="linking">
                  @if (linking) {
                    <span class="spinner-small"></span>
                  } @else {
                    Verknüpfen
                  }
                </button>
              </div>
            </div>
          }
        </div>

        <div class="section">
          <h3>Haushalt</h3>
          @if (user.householdName) {
            <div class="household-info">
              <span class="household-icon">🏠</span>
              <span>{{ user.householdName }}</span>
            </div>
          } @else {
            <p class="no-household">Noch keinem Haushalt beigetreten</p>
          }
        </div>

        <div class="section">
          <h3>Punkte</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ userStats?.totalPoints || 0 }}</span>
              <span class="stat-label">Gesamt</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userStats?.completedTasks || 0 }}</span>
              <span class="stat-label">Erledigt</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userStats?.skippedTasks || 0 }}</span>
              <span class="stat-label">Übersprungen</span>
            </div>
          </div>
        </div>
      } @else {
        <div class="empty-state">
          <p>Bitte melde dich an, um dein Profil zu sehen.</p>
          <button class="btn-primary" routerLink="/login">Anmelden</button>
        </div>
      }

      <button class="btn-logout" (click)="logout()">Abmelden</button>
    </div>
  `,
  styles: [`
    .profile-page {
      padding: 16px;
    }
    
    .header {
      margin-bottom: 24px;
      
      h1 {
        font-size: 1.5rem;
      }
    }
    
    .loading-spinner {
      display: flex;
      justify-content: center;
      padding: 40px;
      
      .spinner {
        width: 40px;
        height: 40px;
        border: 3px solid #e0e0e0;
        border-top-color: #1976d2;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
    }
    
    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
    
    .profile-card {
      text-align: center;
      padding: 32px;
      background: white;
      border-radius: 16px;
      margin-bottom: 24px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      
      .avatar {
        width: 80px;
        height: 80px;
        background: #1976d2;
        color: white;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 2rem;
        font-weight: 600;
        margin: 0 auto 16px;
      }
      
      h2 {
        font-size: 1.3rem;
        margin-bottom: 4px;
      }
      
      .email {
        color: #666;
      }
    }
    
    .section {
      background: white;
      padding: 20px;
      border-radius: 12px;
      margin-bottom: 16px;
      
      h3 {
        font-size: 1rem;
        margin-bottom: 12px;
        color: #333;
      }
    }
    
    .status.connected {
      background: #e8f5e9;
      color: #2e7d32;
      padding: 12px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
    }
    
    .telegram-setup {
      p {
        margin-bottom: 12px;
        color: #666;
      }
      
      ol {
        margin-left: 20px;
        margin-bottom: 16px;
        
        li {
          margin-bottom: 8px;
        }
      }
    }
    
    .code-input {
      display: flex;
      gap: 8px;
      
      input {
        flex: 1;
        padding: 10px;
        border: 1px solid #ddd;
        border-radius: 8px;
        font-size: 1rem;
        text-transform: uppercase;
      }
    }
    
    .spinner-small {
      width: 16px;
      height: 16px;
      border: 2px solid white;
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    
    .household-info {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .household-icon {
        font-size: 1.5rem;
      }
    }
    
    .no-household {
      color: #999;
    }
    
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
    }
    
    .stat-item {
      text-align: center;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 8px;
      
      .stat-value {
        display: block;
        font-size: 1.5rem;
        font-weight: 700;
        color: #1976d2;
      }
      
      .stat-label {
        font-size: 0.8rem;
        color: #666;
      }
    }
    
    .empty-state {
      text-align: center;
      padding: 40px;
      background: #f5f5f5;
      border-radius: 12px;
      
      p {
        color: #666;
        margin-bottom: 16px;
      }
    }
    
    .btn-primary {
      padding: 10px 20px;
      background: #1976d2;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      cursor: pointer;
      
      &:hover:not(:disabled) {
        background: #1565c0;
      }
      
      &:disabled {
        opacity: 0.7;
        cursor: not-allowed;
      }
    }
    
    .btn-logout {
      width: 100%;
      padding: 14px;
      background: #f44336;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      cursor: pointer;
      margin-top: 24px;
      
      &:hover {
        background: #d32f2f;
      }
    }
  `]
})
export class ProfileComponent implements OnInit {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  
  user: User | null = null;
  userStats: { totalPoints: number; completedTasks: number; skippedTasks: number } | null = null;
  verificationCode = '';
  linking = false;
  loading = true;

  ngOnInit() {
    this.loadUser();
  }

  loadUser() {
    this.loading = true;
    
    // First try to get from AuthService
    let user = this.authService.getCurrentUser();
    
    if (user) {
      this.user = user;
      this.loadUserStats();
    } else {
      // Demo user for development
      this.user = {
        id: 1,
        username: 'sascha',
        email: 'sascha@example.com',
        displayName: 'Sascha',
        hasTelegram: false,
        householdId: 1,
        householdName: 'Deeg Haushalt'
      };
      this.userStats = {
        totalPoints: 12,
        completedTasks: 10,
        skippedTasks: 2
      };
    }
    this.loading = false;
  }

  loadUserStats() {
    if (!this.user) return;
    
    const householdId = this.authService.householdId();
    if (!householdId) return;
    
    this.api.getLeaderboard(householdId).subscribe({
      next: (entries) => {
        const myEntry = entries.find(e => e.userId === this.user?.id);
        if (myEntry) {
          this.userStats = {
            totalPoints: myEntry.totalPoints,
            completedTasks: myEntry.completedTasks,
            skippedTasks: myEntry.skippedTasks
          };
        }
      },
      error: () => {
        // Keep existing stats on error
      }
    });
  }

  linkTelegram() {
    if (this.verificationCode.length < 8 || !this.user) return;
    
    this.linking = true;
    // Would link via API
    // For now, just simulate
    setTimeout(() => {
      this.toast.success('Telegram erfolgreich verknüpft!');
      this.user = { ...this.user!, hasTelegram: true };
      this.verificationCode = '';
      this.linking = false;
    }, 1000);
  }

  logout() {
    this.authService.logout();
  }
}
