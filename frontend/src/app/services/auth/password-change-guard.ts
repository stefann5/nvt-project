import { Injectable } from '@angular/core';
import {
  CanActivate,
  Router,
} from '@angular/router';
import { AuthService } from './auth-service';

@Injectable({
  providedIn: 'root'
})
export class PasswordChangeGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) { }

  canActivate(): boolean {
    // If user must change password, redirect to change-password page
    if (this.authService.mustChangePassword()) {
      this.router.navigate(['/change-password']);
      return false;
    }
    return true;
  }
}
