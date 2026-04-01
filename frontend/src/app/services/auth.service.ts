import { Injectable, signal, computed } from '@angular/core';
import { User } from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUser = signal<User | null>(null);

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);
  readonly householdId = computed(() => this.currentUser()?.householdId ?? null);
  readonly userId = computed(() => this.currentUser()?.id ?? null);

  setCurrentUser(user: User): void {
    this.currentUser.set(user);
    // Store in localStorage for persistence
    localStorage.setItem('currentUser', JSON.stringify(user));
  }

  getCurrentUser(): User | null {
    // Try to get from signal first
    let user = this.currentUser();
    
    // If not in signal, try localStorage
    if (!user) {
      const stored = localStorage.getItem('currentUser');
      if (stored) {
        try {
          user = JSON.parse(stored);
          this.currentUser.set(user);
        } catch {
          localStorage.removeItem('currentUser');
        }
      }
    }
    
    return user;
  }

  logout(): void {
    this.currentUser.set(null);
    localStorage.removeItem('currentUser');
    // Redirect to logout endpoint
    window.location.href = '/api/logout';
  }

  // Check if user data is in URL (OAuth callback)
  handleOAuthCallback(): boolean {
    const params = new URLSearchParams(window.location.search);
    const userData = params.get('user');
    
    if (userData) {
      try {
        const user = JSON.parse(decodeURIComponent(userData)) as User;
        this.setCurrentUser(user);
        // Clean URL
        window.history.replaceState({}, document.title, window.location.pathname);
        return true;
      } catch {
        console.error('Failed to parse user data from OAuth callback');
      }
    }
    
    return false;
  }

  // Initialize from storage on app load
  initFromStorage(): void {
    const stored = localStorage.getItem('currentUser');
    if (stored) {
      try {
        const user = JSON.parse(stored) as User;
        this.currentUser.set(user);
      } catch {
        localStorage.removeItem('currentUser');
      }
    }
  }
}
