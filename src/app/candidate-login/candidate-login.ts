import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-candidate-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './candidate-login.html',
  styleUrl: './candidate-login.css'
})
export class CandidateLogin {

  email: string = '';
  password: string = '';
  errorMessage: string = '';

  constructor(private router: Router) {}

  login() {

    this.errorMessage = '';

    // Empty fields
    if (!this.email || !this.password) {
      this.errorMessage = 'Please enter your email and password.';
      return;
    }

    // Get registered user
    const storedUser = localStorage.getItem('hireRankerUser');

    if (!storedUser) {
      this.errorMessage =
        'Account not found. Please create a candidate account first.';
      return;
    }

    const user = JSON.parse(storedUser);

    // Check candidate credentials
    if (
      this.email.trim().toLowerCase() === user.email.toLowerCase() &&
      this.password === user.password
    ) {

      // Successful login
      localStorage.setItem('isLoggedIn', 'true');
      localStorage.setItem('userRole', 'CANDIDATE');

      this.router.navigate(['/candidate/dashboard']);

    } else {

      this.errorMessage =
        'Incorrect email or password. Please enter the correct details.';
    }
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }

  goToMainLogin() {
    this.router.navigate(['/']);
  }
}