import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Task, TaskStatus } from '../../models/task.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    @if (!isAuthenticated) {
      <div class="not-authenticated">
        <div class="login-card">
          <h1>🏠 Haushaltsplaner</h1>
          <p>Faire Aufgabenverteilung im Haushalt</p>
          <a routerLink="/login" class="btn-login">
            <span class="icon">☁️</span>
            Mit Nextcloud anmelden
          </a>
        </div>
      </div>
    } @else {
      <div class="home-page">
        <header class="header">
          <h1>🌅 Guten Morgen, {{ userName }}</h1>
          <p class="date">{{ today | date:'EEEE, d. MMMM yyyy' }}</p>
        </header>

        <section class="today-section">
          <h2>Heute für dich:</h2>
          @if (loading) {
            <div class="loading-spinner">
              <div class="spinner"></div>
            </div>
          } @else if (tasks.length === 0) {
            <div class="empty-state">
              <p>Keine Aufgaben für heute! 🎉</p>
            </div>
          } @else {
            @for (task of tasks; track task.id) {
              <div class="task-item" [class.completed]="task.status === 'COMPLETED'">
                <button 
                  class="task-checkbox" 
                  [class.checked]="task.status === 'COMPLETED'"
                  (click)="completeTask(task)">
                  @if (task.status === 'COMPLETED') {
                    ✓
                  }
                </button>
                <div class="task-content">
                  <span class="task-name">{{ task.name }}</span>
                  <span class="task-meta">{{ task.points }} Punkt{{ task.points > 1 ? 'e' : '' }}</span>
                  @if (task.completionPeriodEnd) {
                    <span class="task-due">Fällig bis {{ task.completionPeriodEnd | date:'d.M.' }}</span>
                  }
                </div>
                <div class="task-actions">
                  @if (task.status === 'PENDING') {
                    <button class="action-btn" (click)="skipTask(task)" title="Überspringen">⏭️</button>
                    <button class="action-btn" (click)="moveTask(task)" title="Verschieben">📅</button>
                  }
                </div>
              </div>
            }
          }
        </section>

        <section class="upcoming-section">
          <h2>Diese Woche:</h2>
          @for (task of upcomingTasks; track task.id) {
            <div class="task-card">
              <span class="task-name">{{ task.name }}</span>
              <span class="task-due">{{ task.dueDate | date:'EEE, d.' }}</span>
            </div>
          }
          @if (upcomingTasks.length === 0) {
            <p class="empty-text">Keine anstehenden Aufgaben</p>
          }
        </section>
      </div>
    }
  `,
  styles: [`
    .not-authenticated {
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
    
    .btn-login {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      width: 100%;
      padding: 16px 24px;
      background: #0082c2;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 500;
      text-decoration: none;
      cursor: pointer;
      transition: background 0.2s;
      
      &:hover {
        background: #006ba3;
      }
      
      .icon {
        font-size: 1.5rem;
      }
    }
    
    .home-page {
      padding: 16px;
    }
    
    .header {
      margin-bottom: 24px;
      
      h1 {
        font-size: 1.5rem;
        font-weight: 600;
        margin-bottom: 4px;
      }
      
      .date {
        color: #666;
        font-size: 0.9rem;
      }
    }
    
    .today-section, .upcoming-section {
      margin-bottom: 24px;
      
      h2 {
        font-size: 1.1rem;
        font-weight: 500;
        margin-bottom: 12px;
        color: #333;
      }
    }
    
    .loading-spinner {
      display: flex;
      justify-content: center;
      padding: 20px;
      
      .spinner {
        width: 32px;
        height: 32px;
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
    
    .empty-state {
      background: #e8f5e9;
      padding: 24px;
      border-radius: 12px;
      text-align: center;
      color: #2e7d32;
    }
    
    .empty-text {
      color: #999;
      font-size: 0.9rem;
    }
    
    .task-item {
      display: flex;
      align-items: center;
      padding: 12px;
      background: white;
      border-radius: 12px;
      margin-bottom: 8px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
      
      &.completed {
        opacity: 0.7;
        background: #f5f5f5;
      }
    }
    
    .task-checkbox {
      width: 28px;
      height: 28px;
      border: 2px solid #ddd;
      border-radius: 50%;
      background: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      color: white;
      transition: all 0.2s;
      margin-right: 12px;
      
      &.checked {
        background: #4caf50;
        border-color: #4caf50;
      }
    }
    
    .task-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      
      .task-name {
        font-weight: 500;
      }
      
      .task-meta {
        color: #666;
        font-size: 0.85rem;
      }
      
      .task-due {
        color: #1976d2;
        font-size: 0.8rem;
        margin-top: 2px;
      }
    }
    
    .task-actions {
      display: flex;
      gap: 4px;
    }
    
    .action-btn {
      background: none;
      border: none;
      font-size: 1.2rem;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 8px;
      transition: background 0.2s;
      
      &:hover {
        background: #e0e0e0;
      }
    }
    
    .task-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: white;
      border-radius: 8px;
      margin-bottom: 8px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
      
      .task-due {
        color: #666;
        font-size: 0.85rem;
      }
    }
  `]
})
export class HomeComponent implements OnInit {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  
  isAuthenticated = false;
  userName = '';
  today = new Date();
  tasks: Task[] = [];
  upcomingTasks: Task[] = [];
  loading = true;

  ngOnInit() {
    this.isAuthenticated = this.authService.isAuthenticated;
    if (this.isAuthenticated) {
      const user = this.authService.getCurrentUser();
      this.userName = user?.displayName || user?.username || 'Gast';
      this.loadTasks();
    } else {
      this.loading = false;
    }
  }

  get currentUserId(): number | null {
    return this.authService.userId || null;
  }

  get householdId(): number | null {
    return this.authService.householdId();
  }

  loadTasks() {
    const userId = this.currentUserId;
    const householdId = this.householdId;
    
    if (!householdId) {
      this.loading = false;
      return;
    }
    
    this.loading = true;
    this.api.getTodaysTasks(householdId).subscribe({
      next: (tasks) => {
        this.tasks = userId ? tasks.filter(t => t.assignedUserId === userId) : tasks;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });

    this.api.getPendingTasks(householdId).subscribe({
      next: (tasks) => {
        this.upcomingTasks = userId 
          ? tasks.filter(t => t.assignedUserId === userId && t.status === 'PENDING').slice(0, 5)
          : [];
      },
      error: () => {
        this.upcomingTasks = [];
      }
    });
  }

  completeTask(task: Task) {
    this.api.completeTask(task.id).subscribe({
      next: () => {
        this.toast.success('Aufgabe erledigt!');
        this.loadTasks();
      },
      error: () => {
        this.toast.error('Fehler beim Erledigen der Aufgabe');
      }
    });
  }

  skipTask(task: Task) {
    this.api.skipTask(task.id).subscribe({
      next: () => {
        this.toast.success('Aufgabe übersprungen');
        this.loadTasks();
      },
      error: () => {
        this.toast.error('Fehler beim Überspringen');
      }
    });
  }

  moveTask(task: Task) {
    this.api.moveTask(task.id).subscribe({
      next: () => {
        this.toast.success('Aufgabe verschoben');
        this.loadTasks();
      },
      error: () => {
        this.toast.error('Fehler beim Verschieben');
      }
    });
  }
}
