import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { RankingService, CandidateRankingItem } from '../services/ranking.service';
import { JobService } from '../services/job.service';

interface RankedCandidate {
  rank: number;
  name: string;
  email: string;
  role: string;
  matchScore: number;
  skillsMatch: number;
  experience: string;
  education: number;
  overallScore: number;
  status: 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Rejected';
}

@Component({
  selector: 'app-candidate-ranking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './candidate-ranking.html',
  styleUrl: './candidate-ranking.css'
})
export class CandidateRanking implements OnInit {
  candidates: RankedCandidate[] = [
    {
      rank: 1,
      name: 'Eshwar Rao',
      email: 'eshwar.rao@email.com',
      role: 'Java Full Stack Developer',
      matchScore: 95,
      skillsMatch: 96,
      experience: '4.5 yrs',
      education: 92,
      overallScore: 95,
      status: 'Shortlisted'
    },
    {
      rank: 2,
      name: 'Krupa Jyothi',
      email: 'krupa.jyothi@email.com',
      role: 'Senior Angular Developer',
      matchScore: 89,
      skillsMatch: 92,
      experience: '3.8 yrs',
      education: 88,
      overallScore: 89,
      status: 'Interview Scheduled'
    },
    {
      rank: 3,
      name: 'Durga Rohith',
      email: 'durga.rohith@email.com',
      role: 'Java Full Stack Developer',
      matchScore: 84,
      skillsMatch: 88,
      experience: '3.2 yrs',
      education: 85,
      overallScore: 84,
      status: 'Under Review'
    },
    {
      rank: 4,
      name: 'Sowmya maloth',
      email: 'sowmya.maloth@email.com',
      role: 'UI/UX Product Designer',
      matchScore: 82,
      skillsMatch: 85,
      experience: '2.9 yrs',
      education: 83,
      overallScore: 82,
      status: 'Under Review'
    },
    {
      rank: 5,
      name: 'Bala Vardhan',
      email: 'bala.vardhan@email.com',
      role: 'Cloud & AI Engineer',
      matchScore: 78,
      skillsMatch: 80,
      experience: '2.5 yrs',
      education: 82,
      overallScore: 78,
      status: 'Rejected'
    }
  ];

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly rankingService: RankingService,
    private readonly jobService: JobService
  ) {}

  ngOnInit(): void {
    const jobIdParam = this.route.snapshot.queryParams['jobId'] || '1';
    const jobId = Number(jobIdParam);
    if (jobId) {
      this.rankingService.getRankingForJob(jobId).subscribe({
        next: (items: CandidateRankingItem[]) => {
          if (items && items.length > 0) {
            this.candidates = items.map((item) => ({
              rank: item.rank,
              name: item.candidateName || `Candidate #${item.candidateId}`,
              email: `candidate${item.candidateId}@hireranker.internal`,
              role: 'Java Full Stack Developer',
              matchScore: Math.round(item.overallScore || 0),
              skillsMatch: Math.round(item.skillsScore || 0),
              experience: `${Math.max(1, Math.round((item.experienceScore || 70) / 20))} yrs`,
              education: Math.round(item.educationScore || 80),
              overallScore: Math.round(item.overallScore || 0),
              status: this.normalizeStatus(item.applicationStatus)
            }));
          }
        },
        error: (err) => {
          console.warn('Backend candidate ranking not accessible, using cached rankings:', err?.status);
        }
      });
    }
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
    this.router.navigate(['/interview-scheduler']);
  }

  viewProfile(c: RankedCandidate): void {
    alert(`Candidate Details:\nName: ${c.name}\nEmail: ${c.email}\nRole: ${c.role}\nAI Overall Score: ${c.overallScore}%\nSkills Match: ${c.skillsMatch}%\nExperience: ${c.experience}\nStatus: ${c.status}`);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}