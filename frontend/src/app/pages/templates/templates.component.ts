import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { TaskTemplate, TaskFrequency } from '../../models/task.model';

@Component({
  selector: 'app-templates',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="templates-page">
      <header class="header">
        <h1>📝 Aufgaben-Vorlagen</h1>
        <button class="btn-primary" (click)="showAddModal = true">+ Neu</button>
      </header>

      @if (loading) {
        <div class="loading-spinner">
          <div class="spinner"></div>
          <p>Lädt...</p>
        </div>
      } @else {
        <div class="template-list">
          @for (template of templates; track template.id) {
            <div class="template-item">
              <div class="template-info">
                <span class="template-name">{{ template.name }}</span>
                <span class="template-meta">
                  {{ getFrequencyLabel(template.frequency) }} • {{ template.defaultPoints }} Pkt.
                </span>
              </div>
              <button class="delete-btn" (click)="deleteTemplate(template.id)">🗑️</button>
            </div>
          }
          @if (templates.length === 0) {
            <div class="empty-state">
              <p>Keine Vorlagen vorhanden</p>
              <p class="hint">Erstelle eine neue Vorlage, um Aufgaben leichter zu planen.</p>
            </div>
          }
        </div>
      }

      @if (showAddModal) {
        <div class="modal-overlay" (click)="showAddModal = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>Neue Vorlage</h2>
            <form (submit)="createTemplate($event)">
              <div class="form-group">
                <label>Name</label>
                <input type="text" [(ngModel)]="newTemplate.name" name="name" required>
              </div>
              <div class="form-group">
                <label>Häufigkeit</label>
                <select [(ngModel)]="newTemplate.frequency" name="frequency">
                  <option value="DAILY">Täglich</option>
                  <option value="WEEKLY">Wöchentlich</option>
                  <option value="BI_WEEKLY">Zwei-Wöchentlich</option>
                  <option value="MONTHLY">Monatlich</option>
                </select>
              </div>
              <div class="form-group">
                <label>Punkte</label>
                <input type="number" [(ngModel)]="newTemplate.defaultPoints" name="points" min="1" value="1">
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
    .templates-page {
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
    
    .empty-state {
      background: #f5f5f5;
      padding: 40px;
      border-radius: 12px;
      text-align: center;
      color: #666;
      
      .hint {
        font-size: 0.9rem;
        color: #999;
        margin-top: 8px;
      }
    }
    
    .template-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: white;
      border-radius: 12px;
      margin-bottom: 8px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    
    .template-info {
      display: flex;
      flex-direction: column;
    }
    
    .template-name {
      font-weight: 500;
    }
    
    .template-meta {
      color: #666;
      font-size: 0.85rem;
    }
    
    .delete-btn {
      background: none;
      border: none;
      font-size: 1.2rem;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 8px;
      transition: background 0.2s;
      
      &:hover {
        background: #ffebee;
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
  `]
})
export class TemplatesComponent implements OnInit {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  
  templates: TaskTemplate[] = [];
  loading = true;
  showAddModal = false;
  
  newTemplate: Partial<TaskTemplate> = {
    name: '',
    frequency: TaskFrequency.DAILY,
    defaultPoints: 1
  };

  ngOnInit() {
    this.loadTemplates();
  }

  get householdId(): number | null {
    return this.authService.householdId();
  }

  loadTemplates() {
    const householdId = this.householdId;
    if (!householdId) {
      this.loading = false;
      this.templates = [];
      return;
    }
    
    this.loading = true;
    this.api.getTemplates(householdId).subscribe({
      next: (templates) => {
        this.templates = templates;
        this.loading = false;
      },
      error: () => {
        this.templates = [];
        this.loading = false;
        this.toast.error('Fehler beim Laden der Vorlagen');
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

  createTemplate(e: Event) {
    e.preventDefault();
    const householdId = this.householdId;
    if (!householdId) {
      this.toast.error('Kein Haushalt gefunden');
      return;
    }
    
    this.api.createTemplate(householdId, this.newTemplate).subscribe({
      next: () => {
        this.toast.success('Vorlage erstellt!');
        this.showAddModal = false;
        this.newTemplate = { name: '', frequency: TaskFrequency.DAILY, defaultPoints: 1 };
        this.loadTemplates();
      },
      error: () => {
        this.toast.error('Fehler beim Erstellen der Vorlage');
      }
    });
  }

  deleteTemplate(id: number) {
    this.api.deleteTemplate(id).subscribe({
      next: () => {
        this.toast.success('Vorlage gelöscht');
        this.loadTemplates();
      },
      error: () => {
        this.toast.error('Fehler beim Löschen der Vorlage');
      }
    });
  }
}
