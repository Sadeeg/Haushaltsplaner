import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { LeaderboardEntry } from '../../models/task.model';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="leaderboard-page">
      <header class="header">
        <h1>🏆 Punktestand</h1>
      </header>

      @if (loading) {
        <div class="loading-spinner">
          <div class="spinner"></div>
          <p>Lädt...</p>
        </div>
      } @else {
        <div class="leaderboard-list">
          @for (entry of leaderboard; track entry.userId; let i = $index) {
            <div class="leaderboard-item" [class.current-user]="entry.userId === currentUserId">
              <div class="rank">
                @if (i === 0) {
                  <span class="medal">🥇</span>
                } @else if (i === 1) {
                  <span class="medal">🥈</span>
                } @else if (i === 2) {
                  <span class="medal">🥉</span>
                } @else {
                  <span class="rank-number">{{ i + 1 }}</span>
                }
              </div>
              <div class="user-info">
                <span class="user-name">{{ entry.displayName }}</span>
                <span class="user-stats">
                  {{ entry.completedTasks }} erledigt • {{ entry.skippedTasks }} übersprungen
                </span>
              </div>
              <div class="points">
                <span class="points-value">{{ entry.totalPoints }}</span>
                <span class="points-label">Punkte</span>
              </div>
            </div>
          }
          @if (leaderboard.length === 0) {
            <div class="empty-state">
              <p>Noch keine Daten vorhanden</p>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .leaderboard-page {
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
      flex-direction: column;
      align-items: center;
      padding: 40px;
      
      .spinner {
        width: 40px;
        height: 40px;
        border: 3px solid #e0e0e0;
        border-top-color: #1976d2;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
      
      p {
        margin-top: 12px;
        color: #666;
      }
    }
    
    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
    
    .empty-state {
      background: #f5f5f5;
      padding: 40px;
      border-radius: 12px;
      text-align: center;
      color: #999;
    }
    
    .leaderboard-item {
      display: flex;
      align-items: center;
      padding: 16px;
      background: white;
      border-radius: 12px;
      margin-bottom: 8px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
      
      &.current-user {
        background: #e3f2fd;
        border: 2px solid #1976d2;
      }
    }
    
    .rank {
      width: 48px;
      text-align: center;
      
      .medal {
        font-size: 2rem;
      }
      
      .rank-number {
        font-size: 1.2rem;
        font-weight: 600;
        color: #666;
      }
    }
    
    .user-info {
      flex: 1;
      margin-left: 12px;
      
      .user-name {
        display: block;
        font-weight: 500;
        font-size: 1.1rem;
      }
      
      .user-stats {
        color: #666;
        font-size: 0.85rem;
      }
    }
    
    .points {
      text-align: center;
      
      .points-value {
        display: block;
        font-size: 1.5rem;
        font-weight: 700;
        color: #1976d2;
      }
      
      .points-label {
        font-size: 0.75rem;
        color: #666;
      }
    }
  `]
})
export class LeaderboardComponent implements OnInit {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  
  leaderboard: LeaderboardEntry[] = [];
  loading = true;

  ngOnInit() {
    this.loadLeaderboard();
  }

  get currentUserId(): number | null {
    return this.authService.userId;
  }

  get householdId(): number | null {
    return this.authService.householdId();
  }

  loadLeaderboard() {
    const householdId = this.householdId;
    
    if (!householdId) {
      this.loading = false;
      // Demo data when no household
      this.leaderboard = [
        { userId: 1, displayName: 'Sascha', totalPoints: 12, completedTasks: 10, skippedTasks: 2 },
        { userId: 2, displayName: 'Alexandra', totalPoints: 10, completedTasks: 8, skippedTasks: 3 }
      ];
      return;
    }
    
    this.loading = true;
    this.api.getLeaderboard(householdId).subscribe({
      next: (entries) => {
        this.leaderboard = entries;
        this.loading = false;
      },
      error: () => {
        // Demo data
        this.leaderboard = [
          { userId: 1, displayName: 'Sascha', totalPoints: 12, completedTasks: 10, skippedTasks: 2 },
          { userId: 2, displayName: 'Alexandra', totalPoints: 10, completedTasks: 8, skippedTasks: 3 }
        ];
        this.loading = false;
      }
    });
  }
}
