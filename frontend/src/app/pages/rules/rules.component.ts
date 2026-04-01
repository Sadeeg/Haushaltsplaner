import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { TaskTemplate, ExclusionRule, ExclusionType } from '../../models/task.model';

@Component({
  selector: 'app-rules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="rules-page">
      <header class="header">
        <h1>⚙️ Regeln</h1>
        <button class="btn-primary" (click)="openAddModal()">+ Neu</button>
      </header>

      @if (loading) {
        <div class="loading-spinner">
          <div class="spinner"></div>
          <p>Lädt...</p>
        </div>
      } @else {
        <div class="rules-list">
          @for (rule of rules; track rule.id) {
            <div class="rule-item">
              <div class="rule-content">
                <span class="task-name">{{ rule.taskAName }}</span>
                <span class="rule-icon">≠</span>
                <span class="task-name">{{ rule.taskBName }}</span>
              </div>
              <button class="delete-btn" (click)="deleteRule(rule.id)">🗑️</button>
            </div>
          }
          @if (rules.length === 0) {
            <div class="empty-state">
              <p>Keine Regeln vorhanden</p>
              <p class="hint">Erstelle Regeln um festzulegen, welche Aufgaben nicht zusammen erledigt werden sollen.</p>
            </div>
          }
        </div>
      }

      <div class="info-card">
        <h3>💡 Beispiel</h3>
        <p><strong>Regel:</strong> "Kochen" ≠ "Katzenklos"</p>
        <p><small>Wenn Sascha kocht, muss er nicht Katzenklos machen.</small></p>
      </div>

      <!-- Add Rule Modal -->
      @if (showAddModal) {
        <div class="modal-overlay" (click)="showAddModal = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>Neue Regel</h2>
            <form (submit)="createRule($event)">
              <div class="form-group">
                <label>Aufgabe A</label>
                <select [(ngModel)]="newRule.taskATemplateId" name="taskA" required>
                  <option value="">Aufgabe auswählen...</option>
                  @for (template of templates; track template.id) {
                    <option [value]="template.id">{{ template.name }}</option>
                  }
                </select>
              </div>
              <div class="form-group">
                <label>ist ausgeschlossen mit</label>
                <div class="arrow-down">↓</div>
              </div>
              <div class="form-group">
                <label>Aufgabe B</label>
                <select [(ngModel)]="newRule.taskBTemplateId" name="taskB" required>
                  <option value="">Aufgabe auswählen...</option>
                  @for (template of templates; track template.id) {
                    <option [value]="template.id">{{ template.name }}</option>
                  }
                </select>
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
    .rules-page {
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
    
    .rules-list {
      margin-bottom: 16px;
    }
    
    .rule-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: white;
      border-radius: 12px;
      margin-bottom: 8px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    
    .rule-content {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .task-name {
        font-weight: 500;
      }
      
      .rule-icon {
        font-size: 1.5rem;
        color: #f44336;
        font-weight: bold;
      }
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
    
    .info-card {
      background: #e3f2fd;
      padding: 16px;
      border-radius: 12px;
      margin-top: 16px;
      
      h3 {
        font-size: 1rem;
        margin-bottom: 8px;
        color: #1976d2;
      }
      
      p {
        font-size: 0.9rem;
        color: #333;
        margin-bottom: 4px;
      }
      
      small {
        color: #666;
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
      
      select {
        width: 100%;
        padding: 10px;
        border: 1px solid #ddd;
        border-radius: 8px;
        font-size: 1rem;
        background: white;
      }
      
      .arrow-down {
        text-align: center;
        font-size: 1.5rem;
        color: #666;
        padding: 4px 0;
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
export class RulesComponent implements OnInit {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  
  rules: ExclusionRule[] = [];
  templates: TaskTemplate[] = [];
  loading = true;
  showAddModal = false;
  
  newRule: Partial<ExclusionRule> = {
    taskATemplateId: 0,
    taskBTemplateId: 0,
    taskAName: '',
    taskBName: '',
    ruleType: ExclusionType.MUTUAL
  };

  ngOnInit() {
    this.loadRules();
    this.loadTemplates();
  }

  get householdId(): number | null {
    return this.authService.householdId();
  }

  loadRules() {
    const householdId = this.householdId;
    if (!householdId) {
      this.loading = false;
      this.rules = [];
      return;
    }
    
    this.loading = true;
    this.api.getRules(householdId).subscribe({
      next: (rules) => {
        this.rules = rules;
        this.loading = false;
      },
      error: () => {
        this.rules = [];
        this.loading = false;
        this.toast.error('Fehler beim Laden der Regeln');
      }
    });
  }

  loadTemplates() {
    const householdId = this.householdId;
    if (!householdId) return;
    
    this.api.getTemplates(householdId).subscribe({
      next: (templates) => {
        this.templates = templates;
      },
      error: () => {
        this.templates = [];
      }
    });
  }

  openAddModal() {
    this.newRule = {
      taskATemplateId: 0,
      taskBTemplateId: 0,
      ruleType: ExclusionType.MUTUAL
    };
    this.showAddModal = true;
  }

  createRule(e: Event) {
    e.preventDefault();
    const householdId = this.householdId;
    if (!householdId) {
      this.toast.error('Kein Haushalt gefunden');
      return;
    }
    
    if (!this.newRule.taskATemplateId || !this.newRule.taskBTemplateId) {
      this.toast.error('Bitte beide Aufgaben auswählen');
      return;
    }
    
    if (this.newRule.taskATemplateId === this.newRule.taskBTemplateId) {
      this.toast.error('Bitte zwei verschiedene Aufgaben auswählen');
      return;
    }
    
    this.api.createRule(householdId, this.newRule as ExclusionRule).subscribe({
      next: () => {
        this.toast.success('Regel erstellt!');
        this.showAddModal = false;
        this.loadRules();
      },
      error: () => {
        this.toast.error('Fehler beim Erstellen der Regel');
      }
    });
  }

  deleteRule(id: number) {
    this.api.deleteRule(id).subscribe({
      next: () => {
        this.toast.success('Regel gelöscht');
        this.loadRules();
      },
      error: () => {
        this.toast.error('Fehler beim Löschen der Regel');
      }
    });
  }
}
