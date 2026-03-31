import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
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

      @if (user) {
        <div class="profile-card">
          <div class="avatar">
            {{ user.displayName?.charAt(0) || '?' }}
          </div>
          <h2>{{ user.displayName }}</h2>
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
                <button class="btn-primary" (click)="linkTelegram()">Verknüpfen</button>
              </div>
            </div>
          }
        </div>

        <div class="section">
          <h3>Haushalt</h3>
          @if (user.householdName) {
            <p>{{ user.householdName }}</p>
          } @else {
            <p class="no-household">Noch keinem Haushalt beigetreten</p>
          }
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
    
    .no-household {
      color: #999;
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
    }
  `]
})
export class ProfileComponent implements OnInit {
  private api = inject(ApiService);
  
  user: User | null = null;
  verificationCode = '';

  ngOnInit() {
    // Demo user
    this.user = {
      id: 1,
      username: 'sascha',
      email: 'sascha@example.com',
      displayName: 'Sascha',
      hasTelegram: false,
      householdId: 1,
      householdName: 'Deeg Haushalt'
    };
  }

  linkTelegram() {
    if (this.verificationCode.length >= 8) {
      // Would link via API
      console.log('Linking with code:', this.verificationCode);
    }
  }

  logout() {
    // OAuth logout
    window.location.href = '/api/logout';
  }
}
