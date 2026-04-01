import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Task, TaskStatus, TaskFrequency, CreateTaskRequest } from '../../models/task.model';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="tasks-page">
      <header class="header">
        <h1>📋 Alle Aufgaben</h1>
        <button class="btn-primary" (click)="showAddModal = true">+ Neu</button>
      </header>

      <div class="filters">
        <button 
          [class.active]="filter === 'all'" 
          (click)="filter = 'all'">Alle</button>
        <button 
          [class.active]="filter === 'pending'" 
          (click)="filter = 'pending'">Offen</button>
        <button 
          [class.active]="filter === 'completed'" 
          (click)="filter = 'completed'">Erledigt</button>
      </div>

      @if (loading) {
        <div class="loading-spinner">
          <div class="spinner"></div>
          <p>Lädt...</p>
        </div>
      } @else {
        <div class="task-list">
          @for (task of filteredTasks; track task.id) {
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
                <span class="task-meta">
                  {{ getFrequencyLabel(task.frequency) }} • {{ task.points }} Pkt.
                </span>
                @if (task.completionPeriodEnd) {
                  <span class="task-due">Fällig bis {{ task.completionPeriodEnd | date:'d.M.' }}</span>
                }
              </div>
              <div class="task-actions">
                @if (task.status === 'PENDING') {
                  <button class="action-btn" (click)="showSkipModal(task)" title="Überspringen/Verschieben">⏭️</button>
                }
              </div>
            </div>
          }
          @if (filteredTasks.length === 0) {
            <p class="empty">Keine Aufgaben gefunden</p>
          }
        </div>
      }

      <!-- Add Task Modal -->
      @if (showAddModal) {
        <div class="modal-overlay" (click)="showAddModal = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>Neue Aufgabe</h2>
            <form (submit)="createTask($event)">
              <div class="form-group">
                <label>Name</label>
                <input type="text" [(ngModel)]="newTask.name" name="name" required>
              </div>
              <div class="form-group">
                <label>Häufigkeit</label>
                <select [(ngModel)]="newTask.frequency" name="frequency">
                  <option value="DAILY">Täglich</option>
                  <option value="WEEKLY">Wöchentlich</option>
                  <option value="BI_WEEKLY">Zwei-Wöchentlich</option>
                  <option value="MONTHLY">Monatlich</option>
                </select>
              </div>
              <div class="form-group">
                <label>Punkte</label>
                <input type="number" [(ngModel)]="newTask.points" name="points" min="1" value="1">
              </div>
              <div class="modal-actions">
                <button type="button" class="btn-secondary" (click)="showAddModal = false">Abbrechen</button>
                <button type="submit" class="btn-primary">Erstellen</button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Skip/Verschieben Modal -->
      @if (showSkipMoveModal && selectedTask) {
        <div class="modal-overlay" (click)="showSkipMoveModal = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>{{ selectedTask.name }}</h2>
            <p class="modal-description">Was möchtest du mit dieser Aufgabe machen?</p>
            
            <div class="skip-options">
              <button class="option-btn" (click)="skipTask()">
                <span class="option-icon">⏭️</span>
                <span class="option-label">Überspringen</span>
                <span class="option-desc">Aufgabe fällt weg, nächste Person übernimmt</span>
              </button>
              
              <button class="option-btn" (click)="moveTask()">
                <span class="option-icon">📅</span>
                <span class="option-label">Verschieben</span>
                <span class="option-desc">Aufgabe wird auf morgen verschoben</span>
              </button>
            </div>
            
            <button class="btn-secondary cancel-btn" (click)="showSkipMoveModal = false">Abbrechen</button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .tasks-page {
      padding: 16px;
    }
    
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      
      h1 {
        font-size: 1.3rem;
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
    
    .filters {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
      
      button {
        padding: 8px 16px;
        border: none;
        background: #e0e0e0;
        border-radius: 20px;
        font-size: 0.9rem;
        cursor: pointer;
        
        &.active {
          background: #1976d2;
          color: white;
        }
      }
    }
    
    .task-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .task-item {
      display: flex;
      align-items: center;
      padding: 12px;
      background: white;
      border-radius: 12px;
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
    
    .empty {
      text-align: center;
      color: #999;
      padding: 40px;
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
        margin-bottom: 8px;
      }
      
      .modal-description {
        color: #666;
        margin-bottom: 20px;
      }
    }
    
    .form-group {
      margin-bottom: 16px;
      
      label {
        display: block;
        margin-bottom: 4px;
        font-weight: 500;
      }
      
      input, select {
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
    
    .btn-primary {
      padding: 10px 20px;
      background: #1976d2;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      cursor: pointer;
      
      &:hover {
        background: #1565c0;
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
    
    .skip-options {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .option-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px;
      background: #f5f5f5;
      border: 2px solid transparent;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s;
      text-align: center;
      
      &:hover {
        background: #e8f5e9;
        border-color: #4caf50;
      }
      
      .option-icon {
        font-size: 2rem;
        margin-bottom: 8px;
      }
      
      .option-label {
        font-weight: 600;
        font-size: 1.1rem;
        margin-bottom: 4px;
      }
      
      .option-desc {
        color: #666;
        font-size: 0.85rem;
      }
    }
    
    .cancel-btn {
      width: 100%;
      margin-top: 16px;
    }
  `]
})
export class TasksComponent implements OnInit {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  filter: 'all' | 'pending' | 'completed' = 'all';
  loading = true;
  showAddModal = false;
  showSkipMoveModal = false;
  selectedTask: Task | null = null;
  
  newTask: Partial<CreateTaskRequest> = {
    name: '',
    frequency: TaskFrequency.DAILY,
    points: 1
  };

  ngOnInit() {
    this.loadTasks();
  }

  get householdId(): number | null {
    return this.authService.householdId();
  }

  loadTasks() {
    const householdId = this.householdId;
    if (!householdId) {
      this.loading = false;
      this.toast.error('Kein Haushalt gefunden. Bitte melde dich erneut an.');
      return;
    }
    
    this.loading = true;
    this.api.getPendingTasks(householdId).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.tasks = [];
        this.filteredTasks = [];
        this.toast.error('Fehler beim Laden der Aufgaben');
      }
    });
  }

  applyFilter() {
    if (this.filter === 'all') {
      this.filteredTasks = this.tasks;
    } else if (this.filter === 'pending') {
      this.filteredTasks = this.tasks.filter(t => t.status === 'PENDING');
    } else {
      this.filteredTasks = this.tasks.filter(t => t.status === 'COMPLETED');
    }
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

  showSkipModal(task: Task) {
    this.selectedTask = task;
    this.showSkipMoveModal = true;
  }

  skipTask() {
    if (!this.selectedTask) return;
    
    this.api.skipTask(this.selectedTask.id).subscribe({
      next: () => {
        this.toast.success('Aufgabe übersprungen');
        this.showSkipMoveModal = false;
        this.selectedTask = null;
        this.loadTasks();
      },
      error: () => {
        this.toast.error('Fehler beim Überspringen der Aufgabe');
      }
    });
  }

  moveTask() {
    if (!this.selectedTask) return;
    
    this.api.moveTask(this.selectedTask.id).subscribe({
      next: () => {
        this.toast.success('Aufgabe auf morgen verschoben');
        this.showSkipMoveModal = false;
        this.selectedTask = null;
        this.loadTasks();
      },
      error: () => {
        this.toast.error('Fehler beim Verschieben der Aufgabe');
      }
    });
  }

  createTask(e: Event) {
    e.preventDefault();
    const householdId = this.householdId;
    if (!householdId) {
      this.toast.error('Kein Haushalt gefunden');
      return;
    }
    
    this.api.createTask(householdId, this.newTask as CreateTaskRequest).subscribe({
      next: () => {
        this.toast.success('Aufgabe erstellt!');
        this.showAddModal = false;
        this.newTask = { name: '', frequency: TaskFrequency.DAILY, points: 1 };
        this.loadTasks();
      },
      error: () => {
        this.toast.error('Fehler beim Erstellen der Aufgabe');
      }
    });
  }

  getFrequencyLabel(frequency: TaskFrequency): string {
    const labels: Record<TaskFrequency, string> = {
      [TaskFrequency.DAILY]: 'Täglich',
      [TaskFrequency.WEEKLY]: 'Wöchentlich',
      [TaskFrequency.BI_WEEKLY]: 'Zwei-Wöchentlich',
      [TaskFrequency.MONTHLY]: 'Monatlich'
    };
    return labels[frequency];
  }
}
