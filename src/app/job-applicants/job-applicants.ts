import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-job-applicants',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './job-applicants.html',
  styleUrl: './job-applicants.css'
})
export class JobApplicants {

  constructor(private readonly router: Router) {}

  applicants = [
    {
      name: 'Eshwar Rao',
      email: 'eshwar.rao@email.com',
      job: 'Java Developer',
      score: 95,
      status: 'Shortlisted'
    },
    {
      name: 'Krupa Jyothi',
      email: 'krupa.jyothi@email.com',
      job: 'Angular Developer',
      score: 89,
      status: 'Shortlisted'
    },
    {
      name: 'Amit Verma',
      email: 'amit.verma@email.com',
      job: 'Java Developer',
      score: 84,
      status: 'Under Review'
    }
  ];

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}