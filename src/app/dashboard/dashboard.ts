import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {

  searchText: string = '';

  candidates = [
    {
      name: 'Eshwar Rao',
      email: 'eshwar.rao@email.com',
      matchScore: 95,
      skills: 96,
      experience: '4.5 yrs',
      education: 92,
      status: 'Shortlisted'
    },
    {
      name: 'Krupa Jyothi',
      email: 'krupa.jyothi@email.com',
      matchScore: 89,
      skills: 92,
      experience: '3.8 yrs',
      education: 88,
      status: 'Shortlisted'
    },
    {
      name: 'Amit Verma',
      email: 'amit.verma@email.com',
      matchScore: 84,
      skills: 88,
      experience: '3.2 yrs',
      education: 85,
      status: 'Shortlisted'
    },
    {
      name: 'Sneha Patel',
      email: 'sneha.patel@email.com',
      matchScore: 82,
      skills: 85,
      experience: '2.9 yrs',
      education: 83,
      status: 'Under Review'
    }
  ];

constructor(private readonly router: Router) {}
  openSection(section: string) {

    switch (section) {

      case 'jobs':
        this.router.navigate(['/job-postings']);
        break;

      case 'applicants':
        this.router.navigate(['/job-applicants']);
        break;

      case 'resume':
        this.router.navigate(['/resume-screening']);
        break;

      case 'criteria':
        this.router.navigate(['/evaluation-criteria']);
        break;

      case 'ranking':
        this.router.navigate(['/candidate-ranking']);
        break;

      case 'shortlisted':
        this.router.navigate(['/shortlisted-candidates']);
        break;

      case 'messages':
        this.router.navigate(['/messages']);
        break;

      case 'interview':
        this.router.navigate(['/interview-scheduler']);
        break;

      case 'analytics':
        this.router.navigate(['/reports']);
        break;

      case 'settings':
        this.router.navigate(['/settings']);
        break;

      default:
        this.router.navigate(['/dashboard']);
    }
  }

  logout() {

    localStorage.removeItem('isLoggedIn');

    this.router.navigate(['/']);
  }
}