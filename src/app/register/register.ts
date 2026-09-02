import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,

  imports: [
    FormsModule,
    CommonModule
  ],

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
  togglePassword() {
  this.showPassword = !this.showPassword;
}
  toggleConfirmPassword() {
  this.showConfirmPassword = !this.showConfirmPassword;
}

  errorMessage: string = '';

  constructor(private router: Router) {}

 createAccount() {

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

  if (!this.acceptTerms) {
    this.errorMessage = 'Please accept the Terms & Conditions.';
    return;
  }

  // Save account
  const user = {
    fullName: this.fullName,
    email: this.email,
    password: this.password
  };

  localStorage.setItem(
    'hireRankerUser',
    JSON.stringify(user)
  );

  alert('Account created successfully!');

  this.router.navigate(['/']);
}
}