import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DashboardService, AdminDashboardData } from '../services/dashboard.service';

interface FunnelStage {
  label: string;
  count: number;
  percentage: number;
  color: string;
}

interface SkillDemand {
  skill: string;
  demand: number;
  applicants: number;
}

interface SourceItem {
  source: string;
  percentage: number;
  count: number;
  color: string;
}

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

  funnelStages: FunnelStage[] = [];
  skillsDemand: SkillDemand[] = [];
  sources: SourceItem[] = [];
  isLoading = true;

  constructor(
    private readonly router: Router,
    private readonly dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.loadReportsData();
  }

  loadReportsData(): void {
    this.isLoading = true;
    this.dashboardService.getAdminDashboard('All Time').subscribe({
      next: (data: AdminDashboardData) => {
        this.isLoading = false;
        if (!data) return;

        const totalApps = data.totalApplications || 0;
        const screened = data.screenedResumes || 0;
        const shortlisted = data.shortlistedCandidates || 0;
        const interviews = data.totalInterviews || 0;
        const avgScore = data.averageMatchScore || 0;

        // Count rejected if present in applicationStatus
        const rejectedItem = data.applicationStatus?.find(
          s => s.status?.toUpperCase() === 'REJECTED'
        );
        const rejectedCount = rejectedItem ? rejectedItem.count : 0;

        this.metrics = {
          totalApplicants: totalApps,
          screenedResumes: screened,
          shortlisted: shortlisted,
          rejected: rejectedCount,
          interviewsHeld: interviews,
          offersExtended: 0,
          avgMatchScore: Math.round(avgScore)
        };

        // Calculate recruitment conversion funnel dynamically without divide-by-zero
        if (totalApps > 0) {
          this.funnelStages = [
            {
              label: 'Total Applications',
              count: totalApps,
              percentage: 100,
              color: '#4f46e5'
            },
            {
              label: 'AI Screened Resumes',
              count: screened,
              percentage: Math.min(100, Math.round((screened / totalApps) * 100)),
              color: '#0ea5e9'
            },
            {
              label: 'Shortlisted for Interview',
              count: shortlisted,
              percentage: Math.min(100, Math.round((shortlisted / totalApps) * 100)),
              color: '#8b5cf6'
            },
            {
              label: 'Completed Assessments',
              count: interviews,
              percentage: Math.min(100, Math.round((interviews / totalApps) * 100)),
              color: '#f59e0b'
            },
            {
              label: 'Job Offers Made',
              count: 0,
              percentage: 0,
              color: '#10b981'
            }
          ];
        } else {
          this.funnelStages = [
            { label: 'Total Applications', count: 0, percentage: 0, color: '#4f46e5' },
            { label: 'AI Screened Resumes', count: 0, percentage: 0, color: '#0ea5e9' },
            { label: 'Shortlisted for Interview', count: 0, percentage: 0, color: '#8b5cf6' },
            { label: 'Completed Assessments', count: 0, percentage: 0, color: '#f59e0b' },
            { label: 'Job Offers Made', count: 0, percentage: 0, color: '#10b981' }
          ];
        }

        // Skills demand from real topSkills
        if (data.topSkills && data.topSkills.length > 0) {
          this.skillsDemand = data.topSkills.map(s => ({
            skill: s.name,
            demand: s.percent,
            applicants: s.count
          }));
        } else {
          this.skillsDemand = [];
        }

        // Sources list empty if no source data tracked in application model
        this.sources = [];
      },
      error: (err) => {
        console.error('Failed to load reports data:', err);
        this.isLoading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}