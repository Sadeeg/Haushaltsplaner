import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User, Task, TaskTemplate, ExclusionRule, LeaderboardEntry, CreateTaskRequest } from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  // Health
  health(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${this.baseUrl}/health`);
  }

  // Users
  getCurrentUser(username: string): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/me`, { params: { username } });
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`);
  }

  generateTelegramCode(userId: number): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}/users/${userId}/telegram/generate-code`, {});
  }

  linkTelegram(code: string, telegramChatId: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/users/telegram/link`, {}, { 
      params: { code, telegramChatId: telegramChatId.toString() } 
    });
  }

  // Tasks
  getTasksForUser(userId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/tasks/user/${userId}`);
  }

  getTasksForUserOnDate(userId: number, date: string): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/tasks/user/${userId}/date/${date}`);
  }

  getTodaysTasks(householdId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/tasks/household/${householdId}/today`);
  }

  getPendingTasks(householdId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.baseUrl}/tasks/household/${householdId}/pending`);
  }

  createTask(householdId: number, task: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(`${this.baseUrl}/tasks/household/${householdId}`, task);
  }

  completeTask(taskId: number): Observable<Task> {
    return this.http.post<Task>(`${this.baseUrl}/tasks/${taskId}/complete`, {});
  }

  skipTask(taskId: number): Observable<Task> {
    return this.http.post<Task>(`${this.baseUrl}/tasks/${taskId}/skip`, {});
  }

  moveTask(taskId: number): Observable<Task> {
    return this.http.post<Task>(`${this.baseUrl}/tasks/${taskId}/move`, {});
  }

  getLeaderboard(householdId: number): Observable<LeaderboardEntry[]> {
    return this.http.get<LeaderboardEntry[]>(`${this.baseUrl}/tasks/household/${householdId}/leaderboard`);
  }

  // Task Templates
  getTemplates(householdId: number): Observable<TaskTemplate[]> {
    return this.http.get<TaskTemplate[]>(`${this.baseUrl}/templates/household/${householdId}`);
  }

  createTemplate(householdId: number, template: Partial<TaskTemplate>): Observable<TaskTemplate> {
    return this.http.post<TaskTemplate>(`${this.baseUrl}/templates/household/${householdId}`, template);
  }

  deleteTemplate(templateId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/templates/${templateId}`);
  }

  // Exclusion Rules
  getRules(householdId: number): Observable<ExclusionRule[]> {
    return this.http.get<ExclusionRule[]>(`${this.baseUrl}/rules/household/${householdId}`);
  }

  createRule(householdId: number, rule: Partial<ExclusionRule>): Observable<ExclusionRule> {
    return this.http.post<ExclusionRule>(`${this.baseUrl}/rules/household/${householdId}`, rule);
  }

  deleteRule(ruleId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rules/${ruleId}`);
  }

  // Household
  createHousehold(name: string): Observable<{ id: number; name: string; inviteCode: string }> {
    return this.http.post<{ id: number; name: string; inviteCode: string }>(`${this.baseUrl}/households`, { name });
  }

  joinHousehold(inviteCode: string): Observable<{ id: number; name: string; inviteCode: string }> {
    return this.http.post<{ id: number; name: string; inviteCode: string }>(`${this.baseUrl}/households/join`, { inviteCode });
  }

  getHouseholdInfo(householdId: number): Observable<{ id: number; name: string; inviteCode: string; memberCount: number }> {
    return this.http.get<{ id: number; name: string; inviteCode: string; memberCount: number }>(`${this.baseUrl}/households/${householdId}`);
  }
}
