import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const user = authService.getCurrentUser();

  // For OAuth with cookies, we mainly pass through
  // But we can add the user ID to the request if needed
  if (user) {
    // Clone the request and add custom headers if needed
    // Since we're using cookies for OAuth, we don't need to add Authorization header
    // But we can add user context headers
    const authReq = req.clone({
      setHeaders: {
        'X-User-Id': user.id.toString(),
        'X-Household-Id': user.householdId?.toString() ?? ''
      }
    });
    return next(authReq);
  }

  return next(req);
};
