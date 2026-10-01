import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RankingService, CandidateRankingItem } from '../services/ranking.service';
import { JobService } from '../services/job.service';

export interface RankedCandidate {
  rank: number;
  candidateId: number;
  applicationId: number;
  name: string;
  email: string;
  role: string;
  matchScore: number;
  skillsMatch: number;
  experience: string;
  education: number;
  overallScore: number;
  status: 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Rejected';
  screenedAt?: string;
  resumeFileName?: string;
}

@Component({
  selector: 'app-candidate-ranking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './candidate-ranking.html',
  styleUrl: './candidate-ranking.css'
})
export class CandidateRanking implements OnInit {
  candidates: RankedCandidate[] = [];
  availableJobs: { id: number; title: string }[] = [];
  selectedJobId: number = 0; // 0 = All Jobs (Global Ranking)
  isLoading = true;
  selectedProfileCandidate: RankedCandidate | null = null;

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly rankingService: RankingService,
    private readonly jobService: JobService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Preload jobs for dropdown filter
    this.jobService.fetchJobsFromBackend().subscribe({
      next: (jobs) => {
        if (jobs && jobs.length > 0) {
          this.availableJobs = jobs.map(j => ({ id: j.id, title: j.title }));
        }
      },
      error: () => {}
    });

    const jobIdParam = this.route.snapshot.queryParams['jobId'];
    if (jobIdParam) {
      this.selectedJobId = Number(jobIdParam);
    }

    this.loadRankings();
  }

  onJobFilterChange(jobId: any): void {
    this.selectedJobId = Number(jobId);
    this.loadRankings();
  }

  loadRankings(): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    if (this.selectedJobId > 0) {
      this.rankingService.getRankingForJob(this.selectedJobId).subscribe({
        next: (items: CandidateRankingItem[]) => {
          this.isLoading = false;
          this.mapRankings(items);
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoading = false;
          this.candidates = [];
          this.cdr.detectChanges();
        }
      });
    } else {
      this.rankingService.getAllRankings().subscribe({
        next: (items: CandidateRankingItem[]) => {
          this.isLoading = false;
          this.mapRankings(items);
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoading = false;
          this.candidates = [];
          this.cdr.detectChanges();
        }
      });
    }
  }

  private mapRankings(items: CandidateRankingItem[]): void {
    if (!items || items.length === 0) {
      this.candidates = [];
      return;
    }

    // Filter out any entries without completed screening score
    const validItems = items.filter(item => item.overallScore !== null && item.overallScore !== undefined);

    // Deterministic sorting: overallScore DESC, skillsScore DESC, experienceScore DESC, applicationId ASC
    validItems.sort((a, b) => {
      const scoreDiff = (b.overallScore || 0) - (a.overallScore || 0);
      if (scoreDiff !== 0) return scoreDiff;

      const skillsDiff = (b.skillsScore || 0) - (a.skillsScore || 0);
      if (skillsDiff !== 0) return skillsDiff;

      const expDiff = (b.experienceScore || 0) - (a.experienceScore || 0);
      if (expDiff !== 0) return expDiff;

      return (a.applicationId || 0) - (b.applicationId || 0);
    });

    this.candidates = validItems.map((item, index) => {
      const jobTitle = item.jobTitle || 'Applied Role';
      const email = item.candidateEmail || (item as any).email || `candidate${item.candidateId}@hireranker.internal`;

      return {
        rank: index + 1,
        candidateId: item.candidateId,
        applicationId: item.applicationId,
        name: item.candidateName || `Candidate #${item.candidateId}`,
        email: email,
        role: jobTitle,
        matchScore: item.overallScore != null ? Math.round(item.overallScore) : 0,
        skillsMatch: item.skillsScore != null ? Math.round(item.skillsScore) : 0,
        experience: item.experienceScore != null ? `${Math.max(1, Math.round(item.experienceScore / 20))} yrs` : '1+ yrs',
        education: item.educationScore != null ? Math.round(item.educationScore) : 0,
        overallScore: item.overallScore != null ? Math.round(item.overallScore) : 0,
        status: this.normalizeStatus(item.applicationStatus),
        screenedAt: item.screenedAt,
        resumeFileName: item.resumeFileName
      };
    });
  }

  private normalizeStatus(rawStatus?: string): 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Rejected' {
    if (!rawStatus) return 'Under Review';
    const s = rawStatus.toUpperCase();
    if (s === 'SHORTLISTED') return 'Shortlisted';
    if (s === 'INTERVIEW' || s === 'INTERVIEW_SCHEDULED') return 'Interview Scheduled';
    if (s === 'REJECTED') return 'Rejected';
    return 'Under Review';
  }

  scheduleInterview(c: RankedCandidate): void {
    this.router.navigate(['/interview-scheduler'], {
      queryParams: {
        candidateId: c.candidateId,
        applicationId: c.applicationId,
        candidateName: c.name
      }
    });
  }

  viewProfile(c: RankedCandidate): void {
    this.selectedProfileCandidate = c;
  }

  closeProfileModal(): void {
    this.selectedProfileCandidate = null;
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}