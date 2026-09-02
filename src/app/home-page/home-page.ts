import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';


@Component({
  selector: 'app-home-page',
  imports: [FormsModule],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  imageUrl = "https://www.hiringhappnz.com/favicon.ico";
  // sideimgurl = "";
   email: string = '';
  password: string = '';

  errorMessage: string = '';

  constructor(private router: Router) {}

  login() {

    this.errorMessage = '';

    // Check empty fields
    if (!this.email || !this.password) {
      this.errorMessage = 'Please enter your email and password.';
      return;
    }

    // Get registered user
    const storedUser = localStorage.getItem('hireRankerUser');

    // No account exists
    if (!storedUser) {
      this.errorMessage =
        'Account not found. Please create an account first.';
      return;
    }

    const user = JSON.parse(storedUser);

    // Check email and password
    if (
      this.email.trim().toLowerCase() === user.email.toLowerCase() &&
      this.password === user.password
    ) {

      // Login successful
      localStorage.setItem('isLoggedIn', 'true');

      this.router.navigate(['/dashboard']);

    } else {

      // Wrong email or password
      this.errorMessage =
        'Incorrect email or password. Please enter the correct details.';

    }
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
  goToCandidateLogin() {
  this.router.navigate(['/candidate-login']);
}
}
