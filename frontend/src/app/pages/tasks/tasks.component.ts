import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
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
        <p>Lädt...</p>
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
                  {{ task.frequency }} • {{ task.dueDate | date:'d.M.' }} • {{ task.points }} Pkt.
                </span>
              </div>
              <div class="task-actions">
                <button class="action-btn" (click)="skipTask(task)">⏭️</button>
                <button class="action-btn" (click)="moveTask(task)">📅</button>
              </div>
            </div>
          }
          @if (filteredTasks.length === 0) {
            <p class="empty">Keine Aufgaben gefunden</p>
          }
        </div>
      }

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
  `]
})
export class TasksComponent implements OnInit {
  private api = inject(ApiService);
  
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  filter: 'all' | 'pending' | 'completed' = 'all';
  loading = true;
  showAddModal = false;
  householdId = 1;
  
  newTask: Partial<CreateTaskRequest> = {
    name: '',
    frequency: TaskFrequency.DAILY,
    points: 1
  };

  ngOnInit() {
    this.loadTasks();
  }

  loadTasks() {
    this.loading = true;
    this.api.getPendingTasks(this.householdId).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.tasks = [];
        this.filteredTasks = [];
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
    this.api.completeTask(task.id).subscribe(() => this.loadTasks());
  }

  skipTask(task: Task) {
    this.api.skipTask(task.id).subscribe(() => this.loadTasks());
  }

  moveTask(task: Task) {
    this.api.moveTask(task.id).subscribe(() => this.loadTasks());
  }

  createTask(e: Event) {
    e.preventDefault();
    this.api.createTask(this.householdId, this.newTask as CreateTaskRequest).subscribe({
      next: () => {
        this.showAddModal = false;
        this.newTask = { name: '', frequency: TaskFrequency.DAILY, points: 1 };
        this.loadTasks();
      }
    });
  }
}
