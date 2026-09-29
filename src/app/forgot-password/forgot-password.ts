import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPassword {
  email: string = '';
  isLoading: boolean = false;
  submitted: boolean = false;
  infoMessage: string = '';
  errorMessage: string = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  sendResetLink(): void {
    const trimmedEmail = (this.email || '').trim();

    if (!trimmedEmail) {
      this.errorMessage = 'Please enter your registered email address.';
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(trimmedEmail)) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.forgotPassword(trimmedEmail).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res && res.success) {
          this.submitted = true;
          this.infoMessage = res.message || 'Password reset link sent. Please check your email.';
        } else {
          // Controlled response with error message
          this.errorMessage = res?.message || 'Unable to send reset link. Please try again.';
        }
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 0) {
          this.errorMessage = 'Unable to connect to the server. Please try again.';
        } else {
          this.errorMessage = 'Unable to send reset link. Please try again.';
        }
      }
    });
  }
}
