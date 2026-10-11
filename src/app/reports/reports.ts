import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DashboardService, ReportData, FunnelStage, SkillDemand, AcquisitionSource } from '../services/dashboard.service';

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
  sources: AcquisitionSource[] = [];
  isLoading = true;
  isError = false;

  constructor(
    private readonly router: Router,
    private readonly dashboardService: DashboardService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadReportsData();
  }

  loadReportsData(): void {
    this.isLoading = true;
    this.isError = false;
    this.cdr.markForCheck();

    this.dashboardService.getReportsData().subscribe({
      next: (data: ReportData) => {
        this.isLoading = false;
        this.isError = false;

        if (data) {
          const totalApps = data.totalApplications || 0;
          const screened = data.screenedResumes || 0;
          const shortlisted = data.shortlistedCandidates || 0;
          const interviews = data.totalInterviews || 0;
          const offers = data.offersExtended || 0;
          const rejected = data.rejectedCandidates || 0;
          const avgScore = data.averageMatchScore || 0;

          this.metrics = {
            totalApplicants: totalApps,
            screenedResumes: screened,
            shortlisted: shortlisted,
            rejected: rejected,
            interviewsHeld: interviews,
            offersExtended: offers,
            avgMatchScore: Math.round(avgScore)
          };

          this.funnelStages = data.funnelStages || [];
          this.sources = data.sources || [];
          this.skillsDemand = data.skillsDemand || [];
        }

        // Trigger change detection for zoneless runtime
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load reports data:', err);
        this.isLoading = false;
        this.isError = true;
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}