import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { Task, TaskStatus } from '../../models/task.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="home-page">
      <header class="header">
        <h1>🌅 Guten Morgen, {{ userName }}</h1>
        <p class="date">{{ today | date:'EEEE, d. MMMM yyyy' }}</p>
      </header>

      <section class="today-section">
        <h2>Heute für dich:</h2>
        @if (loading) {
          <p>Lädt...</p>
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
              </div>
              <div class="task-actions">
                <button class="action-btn" (click)="skipTask(task)" title="Überspringen">⏭️</button>
                <button class="action-btn" (click)="moveTask(task)" title="Verschieben">📅</button>
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
  `,
  styles: [`
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
  
  userName = 'Sascha';
  today = new Date();
  tasks: Task[] = [];
  upcomingTasks: Task[] = [];
  loading = true;
  currentUserId = 1;
  householdId = 1;

  ngOnInit() {
    this.loadTasks();
  }

  loadTasks() {
    this.loading = true;
    this.api.getTodaysTasks(this.householdId).subscribe({
      next: (tasks) => {
        this.tasks = tasks.filter(t => t.assignedUserId === this.currentUserId);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        // Demo data
        this.tasks = [
          { id: 1, name: 'Kochen', frequency: 'DAILY' as any, dueDate: new Date().toISOString(), completionPeriodStart: null, completionPeriodEnd: null, status: 'PENDING' as any, assignedUserId: 1, assignedUserName: 'Sascha', points: 1, completedAt: null },
          { id: 2, name: 'Müll rausbringen', frequency: 'WEEKLY' as any, dueDate: new Date().toISOString(), completionPeriodStart: null, completionPeriodEnd: null, status: 'PENDING' as any, assignedUserId: 1, assignedUserName: 'Sascha', points: 2, completedAt: null }
        ];
      }
    });

    this.api.getPendingTasks(this.householdId).subscribe({
      next: (tasks) => {
        this.upcomingTasks = tasks.filter(t => t.assignedUserId === this.currentUserId && t.status === 'PENDING').slice(0, 5);
      },
      error: () => {
        this.upcomingTasks = [];
      }
    });
  }

  completeTask(task: Task) {
    this.api.completeTask(task.id).subscribe({
      next: () => {
        this.loadTasks();
      }
    });
  }

  skipTask(task: Task) {
    this.api.skipTask(task.id).subscribe({
      next: () => {
        this.loadTasks();
      }
    });
  }

  moveTask(task: Task) {
    this.api.moveTask(task.id).subscribe({
      next: () => {
        this.loadTasks();
      }
    });
  }
}
