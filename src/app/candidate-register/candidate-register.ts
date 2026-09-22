import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-candidate-register',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './candidate-register.html',
  styleUrl: './candidate-register.css'
})
export class CandidateRegister {
  fullName = '';
  email = '';
  password = '';
  confirmPassword = '';
  errorMessage = '';
  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  createAccount(): void {
    this.errorMessage = '';

    if (!this.fullName || !this.email || !this.password || !this.confirmPassword) {
      this.errorMessage = 'Please fill in all fields.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    if (this.password.length < 6) {
      this.errorMessage = 'Password must contain at least 6 characters.';
      return;
    }

    const trimmedName = this.fullName.trim();
    const trimmedEmail = this.email.trim().toLowerCase();
    const trimmedPassword = this.password.trim();

    if (this.authService.isBrowser()) {
      localStorage.setItem('hireRankerCandidate', JSON.stringify({
        fullName: trimmedName,
        email: trimmedEmail,
        password: trimmedPassword
      }));
    }

    // Call backend registration POST /api/auth/register
    this.authService.register(trimmedName, trimmedEmail, trimmedPassword, 'CANDIDATE').subscribe({
      next: (res) => {
        if (res && res.success !== false) {
          this.router.navigate(['/candidate-login']);
        } else {
          this.errorMessage = res?.message || 'Registration failed.';
        }
      },
      error: (err) => {
        const errorMsg: string = err?.error?.message || '';
        if (errorMsg.includes('already exists')) {
          // Account already registered in backend: allow candidate to proceed to login
          this.router.navigate(['/candidate-login']);
        } else {
          // If backend is offline or validation failed, fallback mock registration succeeds locally
          if (this.authService.isBrowser()) {
            localStorage.setItem('hireRankerCandidate', JSON.stringify({
              fullName: trimmedName,
              email: trimmedEmail,
              password: trimmedPassword
            }));
            this.router.navigate(['/candidate-login']);
            return;
          }
          this.errorMessage = errorMsg || 'Registration failed. Please check your details.';
        }
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/candidate-login']);
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}
