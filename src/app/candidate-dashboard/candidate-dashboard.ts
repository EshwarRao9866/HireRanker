import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './candidate-dashboard.html',
  styleUrl: './candidate-dashboard.css'
})
export class CandidateDashboard {

  candidateName = 'Eshwar';

  appliedJobs = 5;
  screenedJobs = 3;
  shortlistedJobs = 1;
  interviews = 1;

  constructor(private router: Router) {}

  openJobs(): void {
    this.router.navigate(['/candidate-jobs']);
  }

  openApplications(): void {
    this.router.navigate(['/my-applications']);
  }

  openResume(): void {
    this.router.navigate(['/resume-upload']);
  }

  openInterviews(): void {
    this.router.navigate(['/candidate-interview']);
  }

  openProfile(): void {
    this.router.navigate(['/candidate-profile']);
  }

  logout(): void {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('candidateLoggedIn');

    this.router.navigate(['/']);
  }
}