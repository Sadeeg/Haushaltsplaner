import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private oauthService = inject(OAuthService);
  private router = inject(Router);

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private authConfig: AuthConfig = {
    issuer: environment.oidcIssuer,
    redirectUri: environment.oidcRedirectUri,
    clientId: environment.oidcClientId,
    responseType: 'code',
    scope: environment.oidcScope,
    showDebugInformation: !environment.production,
    clearHashAfterLogin: true,
    requireHttps: environment.production,
    useSilentRefresh: false,
    sessionChecksEnabled: false
  };

  constructor() {
    this.initialize();
  }

  initFromStorage(): void {
    // OAuth initialization is handled in constructor
    // This method exists for compatibility
  }

  async initialize(): Promise<void> {
    this.oauthService.configure(this.authConfig);

    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      this.isAuthenticatedSubject.next(this.oauthService.hasValidAccessToken());

      this.oauthService.events.subscribe(event => {
        if (event.type === 'token_received' || event.type === 'token_refreshed') {
          this.isAuthenticatedSubject.next(true);
        } else if (event.type === 'logout') {
          this.isAuthenticatedSubject.next(false);
        }
      });
    } catch (error) {
      console.error('OAuth initialization error:', error);
      this.isAuthenticatedSubject.next(false);
    }
  }

  login(): void {
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/']);
  }

  get isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  get accessToken(): string {
    return this.oauthService.getAccessToken();
  }

  get username(): string {
    const claims = this.oauthService.getIdentityClaims() as Record<string, unknown>;
    if (!claims) return '';
    return (claims['preferred_username'] as string)
      || (claims['name'] as string)
      || (claims['sub'] as string)
      || '';
  }

  get userEmail(): string {
    const claims = this.oauthService.getIdentityClaims() as Record<string, unknown>;
    if (!claims) return '';
    return (claims['email'] as string) || '';
  }

  get userId(): number {
    const claims = this.oauthService.getIdentityClaims() as Record<string, unknown>;
    if (!claims) return 0;
    // Nextcloud OIDC sub claim is the user id
    return parseInt((claims['sub'] as string) || '0', 10);
  }

  getCurrentUser(): { id: number; username: string; email: string; displayName: string; hasTelegram: boolean; householdId: number | null; householdName: string | null } | null {
    if (!this.isAuthenticated) return null;
    return {
      id: this.userId,
      username: this.username,
      email: this.userEmail,
      displayName: this.username,
      hasTelegram: false,
      householdId: 1,
      householdName: 'Haushalt'
    };
  }

  householdId(): number {
    // For now, return 1 as default. In a real app, this would come from the backend after user creation
    return 1;
  }
}
