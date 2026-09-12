import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  fullName: string = '';
  email: string = '';
  mobile: string = '';
  password: string = '';
  confirmPassword: string = '';
  acceptTerms: boolean = false;
  showPassword: boolean = false;
  showConfirmPassword: boolean = false;
  errorMessage: string = '';

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  createAccount(): void {
    this.errorMessage = '';

    if (!this.fullName || !this.email || !this.password || !this.confirmPassword) {
      this.errorMessage = 'Please fill in all required fields.';
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

    if (!this.acceptTerms) {
      this.errorMessage = 'Please accept the Terms & Conditions.';
      return;
    }

    const user = {
      fullName: this.fullName,
      email: this.email,
      mobile: this.mobile,
      password: this.password
    };

    if (this.authService.isBrowser()) {
      localStorage.setItem('hireRankerUser', JSON.stringify(user));
    }

    // Call backend registration POST /api/auth/register
    this.authService.register(this.fullName, this.email, this.password, 'ADMIN').subscribe({
      next: (res) => {
        if (res && res.success !== false) {
          alert('Admin account created successfully! Please sign in.');
          this.router.navigate(['/']);
        } else {
          this.errorMessage = res.message || 'Registration failed.';
        }
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Registration failed. Please check your details.';
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/']);
  }
}