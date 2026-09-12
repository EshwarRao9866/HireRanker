import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-candidate-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './candidate-login.html',
  styleUrl: './candidate-login.css'
})
export class CandidateLogin implements OnInit {
  email: string = '';
  password: string = '';
  errorMessage: string = '';
  isLoading: boolean = false;
  showPassword: boolean = false;

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    // Application always opens at login screen on startup
  }

  login(): void {
    this.errorMessage = '';

    if (!this.email || !this.password) {
      this.errorMessage = 'Please enter your candidate email and password.';
      return;
    }

    this.isLoading = true;
    this.authService.login(this.email, this.password).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.token) {
          if (res.role === 'ADMIN') {
            this.router.navigate(['/dashboard']);
          } else {
            this.router.navigate(['/candidate-dashboard']);
          }
        } else {
          this.errorMessage = res.message || 'Login failed.';
        }
      },
      error: (err) => {
        const fallback = this.authService.candidateLogin(this.email, this.password);
        this.isLoading = false;
        if (fallback.success) {
          this.router.navigate(['/candidate-dashboard']);
        } else {
          this.errorMessage = err?.error?.message || fallback.message || 'Incorrect candidate email or password.';
        }
      }
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  goToRegister(): void {
    this.router.navigate(['/candidate-register']);
  }

  goToMainLogin(): void {
    this.router.navigate(['/']);
  }
}