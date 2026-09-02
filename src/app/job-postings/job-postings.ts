import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-job-postings',
  standalone: true,
  templateUrl: './job-postings.html',
  imports: [CommonModule],
  styleUrl: './job-postings.css'
})
export class JobPostings {

  constructor(private readonly router: Router) {}

  readonly jobs = [
    {
      title: 'Java Full Stack Developer',
      department: 'Engineering',
      location: 'Hyderabad',
      applicants: 42,
      status: 'Active'
    },
    {
      title: 'Angular Developer',
      department: 'Engineering',
      location: 'Bangalore',
      applicants: 31,
      status: 'Active'
    },
    {
      title: 'UI/UX Designer',
      department: 'Design',
      location: 'Remote',
      applicants: 18,
      status: 'Closed'
    }
  ];

  createJob() {
    alert('Create Job feature will be connected to the backend.');
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}