import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class Reports {
  metrics = {
    totalApplicants: 248,
    screenedResumes: 186,
    shortlisted: 32,
    rejected: 30,
    interviewsHeld: 24,
    offersExtended: 8,
    avgMatchScore: 78
  };

  funnelStages = [
    { label: 'Total Applications', count: 248, percentage: 100, color: '#4f46e5' },
    { label: 'AI Screened Resumes', count: 186, percentage: 75, color: '#0ea5e9' },
    { label: 'Shortlisted for Interview', count: 32, percentage: 17, color: '#8b5cf6' },
    { label: 'Completed Assessments', count: 24, percentage: 13, color: '#f59e0b' },
    { label: 'Job Offers Made', count: 8, percentage: 4, color: '#10b981' }
  ];

  skillsDemand = [
    { skill: 'Java & Spring Boot', demand: 94, applicants: 112 },
    { skill: 'Angular & TypeScript', demand: 88, applicants: 96 },
    { skill: 'SQL & Database Design', demand: 82, applicants: 85 },
    { skill: 'Microservices & Docker', demand: 76, applicants: 68 },
    { skill: 'Cloud & AI Technologies', demand: 71, applicants: 54 }
  ];

  sources = [
    { source: 'LinkedIn Job Board', percentage: 45, count: 112, color: '#0284c7' },
    { source: 'HireRanker Direct Portal', percentage: 30, count: 74, color: '#4f46e5' },
    { source: 'Employee Referrals', percentage: 15, count: 37, color: '#10b981' },
    { source: 'Campus Drives & Other', percentage: 10, count: 25, color: '#f59e0b' }
  ];

  constructor(private readonly router: Router) {}

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}