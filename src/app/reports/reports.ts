import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DashboardService, AdminDashboardData } from '../services/dashboard.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class Reports implements OnInit {
  metrics = {
    totalApplicants: 0,
    screenedResumes: 0,
    shortlisted: 0,
    rejected: 0,
    interviewsHeld: 0,
    offersExtended: 0,
    avgMatchScore: 0
  };

  funnelStages: Array<{ label: string; count: number; percentage: number; color: string }> = [];

  skillsDemand: Array<{ skill: string; demand: number; applicants: number }> = [];

  sources: Array<{ source: string; percentage: number; count: number; color: string }> = [];

  constructor(
    private readonly router: Router,
    private readonly dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.dashboardService.getAdminDashboard().subscribe({
      next: (data: AdminDashboardData) => {
        if (data) {
          const totalApps = data.totalApplications || 0;
          const screened = data.screenedResumes || 0;
          const shortlisted = data.shortlistedCandidates || 0;
          const interviews = data.totalInterviews || 0;
          const avgScore = data.averageMatchScore || 0;

          this.metrics = {
            totalApplicants: totalApps,
            screenedResumes: screened,
            shortlisted: shortlisted,
            rejected: 0,
            interviewsHeld: interviews,
            offersExtended: 0,
            avgMatchScore: avgScore
          };

          if (totalApps > 0) {
            this.funnelStages = [
              { label: 'Total Applications', count: totalApps, percentage: 100, color: '#4f46e5' },
              { label: 'AI Screened Resumes', count: screened, percentage: Math.round((screened / totalApps) * 100), color: '#0ea5e9' },
              { label: 'Shortlisted for Interview', count: shortlisted, percentage: Math.round((shortlisted / totalApps) * 100), color: '#8b5cf6' },
              { label: 'Completed Assessments', count: interviews, percentage: Math.round((interviews / totalApps) * 100), color: '#f59e0b' },
              { label: 'Job Offers Made', count: 0, percentage: 0, color: '#10b981' }
            ];
          } else {
            this.funnelStages = [];
          }

          if (data.topSkills && data.topSkills.length > 0) {
            this.skillsDemand = data.topSkills.map(s => ({
              skill: s.name,
              demand: s.percent,
              applicants: s.count
            }));
          } else {
            this.skillsDemand = [];
          }
        }
      },
      error: () => {}
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}