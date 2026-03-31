import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
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
        <p>Lädt...</p>
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
            <p class="empty">Keine Vorlagen vorhanden</p>
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
export class TemplatesComponent implements OnInit {
  private api = inject(ApiService);
  
  templates: TaskTemplate[] = [];
  loading = true;
  showAddModal = false;
  householdId = 1;
  
  newTemplate: Partial<TaskTemplate> = {
    name: '',
    frequency: TaskFrequency.DAILY,
    defaultPoints: 1
  };

  ngOnInit() {
    this.loadTemplates();
  }

  loadTemplates() {
    this.loading = true;
    this.api.getTemplates(this.householdId).subscribe({
      next: (templates) => {
        this.templates = templates;
        this.loading = false;
      },
      error: () => {
        this.templates = [];
        this.loading = false;
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
    this.api.createTemplate(this.householdId, this.newTemplate).subscribe({
      next: () => {
        this.showAddModal = false;
        this.newTemplate = { name: '', frequency: TaskFrequency.DAILY, defaultPoints: 1 };
        this.loadTemplates();
      }
    });
  }

  deleteTemplate(id: number) {
    this.api.deleteTemplate(id).subscribe(() => this.loadTemplates());
  }
}
