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
          <h3>Haushalt</h3>
          @if (user.householdName) {
            <div class="household-info">
              <span class="household-icon">🏠</span>
              <div class="household-details">
                <span class="household-name">{{ user.householdName }}</span>
                <span class="household-members">{{ householdMemberCount }} Mitglied(er)</span>
              </div>
            </div>
            
            @if (householdInviteCode) {
              <div class="invite-code-section">
                <h4>Einladungscode</h4>
                <div class="invite-code">
                  <span class="code">{{ householdInviteCode }}</span>
                  <button class="btn-copy" (click)="copyInviteCode()">📋</button>
                </div>
                <p class="hint">Teile diesen Code mit anderen, um sie in den Haushalt einzuladen.</p>
              </div>
            }
            
            <button class="btn-danger-text" (click)="leaveHousehold()">Haushalt verlassen</button>
          } @else {
            <p class="no-household">Noch keinem Haushalt beigetreten</p>
          }
          
          @if (!showJoinForm && !showCreateModal) {
            <div class="household-actions">
              <button class="btn-primary" (click)="showCreateModal = true">Haushalt erstellen</button>
              <button class="btn-secondary" (click)="showJoinForm = true">Haushalt beitreten</button>
            </div>
          }
          
          @if (showJoinForm) {
            <div class="join-form">
              <label>Einladungscode eingeben</label>
              <div class="code-input-row">
                <input type="text" [(ngModel)]="joinCode" placeholder="CODE" maxlength="10" class="code-input">
                <button class="btn-primary" (click)="joinHousehold()" [disabled]="joining">
                  @if (joining) {
                    <span class="spinner-small"></span>
                  } @else {
                    Beitreten
                  }
                </button>
              </div>
              <button class="btn-text" (click)="showJoinForm = false">Abbrechen</button>
            </div>
          }
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
          <h3>Punkte (Monatlich)</h3>
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

      <!-- Create Household Modal -->
      @if (showCreateModal) {
        <div class="modal-overlay" (click)="showCreateModal = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>🏠 Haushalt erstellen</h2>
            <form (submit)="createHousehold($event)">
              <div class="form-group">
                <label>Haushaltsname</label>
                <input type="text" [(ngModel)]="newHouseholdName" name="name" required placeholder="z.B. Deeg Haushalt">
              </div>
              <div class="modal-actions">
                <button type="button" class="btn-secondary" (click)="showCreateModal = false">Abbrechen</button>
                <button type="submit" class="btn-primary" [disabled]="creating">
                  @if (creating) {
                    <span class="spinner-small"></span>
                  } @else {
                    Erstellen
                  }
                </button>
              </div>
            </form>
          </div>
        </div>
      }
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
      
      h4 {
        font-size: 0.9rem;
        color: #666;
        margin-bottom: 8px;
      }
    }
    
    .household-info {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      
      .household-icon {
        font-size: 2rem;
      }
      
      .household-details {
        display: flex;
        flex-direction: column;
        
        .household-name {
          font-weight: 600;
          font-size: 1.1rem;
        }
        
        .household-members {
          color: #666;
          font-size: 0.85rem;
        }
      }
    }
    
    .invite-code-section {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 12px;
      margin-top: 12px;
      
      h4 {
        margin-bottom: 8px;
      }
      
      .invite-code {
        display: flex;
        align-items: center;
        gap: 8px;
        
        .code {
          font-family: monospace;
          font-size: 1.3rem;
          font-weight: 700;
          letter-spacing: 2px;
          color: #1976d2;
          background: white;
          padding: 8px 16px;
          border-radius: 8px;
          border: 2px solid #e0e0e0;
        }
        
        .btn-copy {
          background: none;
          border: none;
          font-size: 1.2rem;
          cursor: pointer;
          padding: 8px;
          border-radius: 8px;
          
          &:hover {
            background: #e0e0e0;
          }
        }
      }
      
      .hint {
        font-size: 0.8rem;
        color: #666;
        margin-top: 8px;
      }
    }
    
    .no-household {
      color: #999;
      margin-bottom: 16px;
    }
    
    .household-actions {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .join-form {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 12px;
      
      label {
        display: block;
        font-weight: 500;
        margin-bottom: 8px;
      }
      
      .code-input-row {
        display: flex;
        gap: 8px;
        
        .code-input {
          flex: 1;
          padding: 10px;
          border: 1px solid #ddd;
          border-radius: 8px;
          font-size: 1rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 2px;
        }
      }
      
      .btn-text {
        background: none;
        border: none;
        color: #666;
        font-size: 0.9rem;
        cursor: pointer;
        margin-top: 8px;
        padding: 4px;
        
        &:hover {
          color: #333;
        }
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
    
    .btn-secondary {
      padding: 10px 20px;
      background: #e0e0e0;
      color: #333;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      cursor: pointer;
      
      &:hover {
        background: #d0d0d0;
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
    
    .btn-danger-text {
      background: none;
      border: none;
      color: #f44336;
      font-size: 0.9rem;
      cursor: pointer;
      padding: 8px 0;
      margin-top: 8px;
      
      &:hover {
        text-decoration: underline;
      }
    }
    
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    
    .modal {
      background: white;
      padding: 24px;
      border-radius: 16px;
      width: 90%;
      max-width: 400px;
      
      h2 {
        margin-bottom: 16px;
      }
    }
    
    .form-group {
      margin-bottom: 16px;
      
      label {
        display: block;
        margin-bottom: 4px;
        font-weight: 500;
      }
      
      input {
        width: 100%;
        padding: 10px;
        border: 1px solid #ddd;
        border-radius: 8px;
        font-size: 1rem;
      }
    }
    
    .modal-actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
      margin-top: 16px;
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
  
  // Household
  householdInviteCode = '';
  householdMemberCount = 0;
  showCreateModal = false;
  showJoinForm = false;
  newHouseholdName = '';
  joinCode = '';
  creating = false;
  joining = false;

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
      this.loadHouseholdInfo();
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
      this.householdInviteCode = 'KERSBACH';
      this.householdMemberCount = 2;
    }
    this.loading = false;
  }

  loadHouseholdInfo() {
    const householdId = this.authService.householdId();
    if (!householdId) return;
    
    this.api.getHouseholdInfo(householdId).subscribe({
      next: (info) => {
        this.householdInviteCode = info.inviteCode;
        this.householdMemberCount = info.memberCount;
      },
      error: () => {
        // Ignore
      }
    });
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

  createHousehold(e: Event) {
    e.preventDefault();
    if (!this.newHouseholdName.trim()) return;
    
    this.creating = true;
    this.api.createHousehold(this.newHouseholdName).subscribe({
      next: (household) => {
        this.toast.success('Haushalt erstellt!');
        this.householdInviteCode = household.inviteCode;
        this.householdMemberCount = 1;
        this.user = { ...this.user!, householdId: household.id, householdName: household.name };
        this.showCreateModal = false;
        this.newHouseholdName = '';
        this.creating = false;
      },
      error: () => {
        this.toast.error('Fehler beim Erstellen des Haushalts');
        this.creating = false;
      }
    });
  }

  joinHousehold() {
    if (!this.joinCode.trim()) return;
    
    this.joining = true;
    this.api.joinHousehold(this.joinCode.toUpperCase()).subscribe({
      next: (household) => {
        this.toast.success('Haushalt beigetreten!');
        this.householdInviteCode = household.inviteCode;
        this.user = { ...this.user!, householdId: household.id, householdName: household.name };
        this.showJoinForm = false;
        this.joinCode = '';
        this.joining = false;
        this.loadHouseholdInfo();
      },
      error: () => {
        this.toast.error('Ungültiger Einladungscode');
        this.joining = false;
      }
    });
  }

  copyInviteCode() {
    navigator.clipboard.writeText(this.householdInviteCode).then(() => {
      this.toast.success('Code kopiert!');
    });
  }

  linkTelegram() {
    if (this.verificationCode.length < 8 || !this.user) return;
    
    this.linking = true;
    // Would link via API
    setTimeout(() => {
      this.toast.success('Telegram erfolgreich verknüpft!');
      this.user = { ...this.user!, hasTelegram: true };
      this.verificationCode = '';
      this.linking = false;
    }, 1000);
  }

  leaveHousehold() {
    if (!confirm('Möchtest du den Haushalt wirklich verlassen?')) return;
    
    this.api.leaveHousehold().subscribe({
      next: () => {
        this.toast.success('Haushalt verlassen');
        this.user = { ...this.user!, householdId: null, householdName: null };
        this.householdInviteCode = '';
        this.householdMemberCount = 0;
      },
      error: () => {
        this.toast.error('Fehler beim Verlassen des Haushalts');
      }
    });
  }

  logout() {
    this.authService.logout();
  }
}
