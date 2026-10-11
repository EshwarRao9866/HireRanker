import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { JobService, JobItem } from '../services/job.service';
import { CandidateService } from '../services/candidate.service';
import { ResumeService } from '../services/resume.service';
import { ApplicationService, ApplicationResponse } from '../services/application.service';

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './job-list.html',
  styleUrl: './job-list.css',
})
export class JobList implements OnInit {
  searchQuery = '';
  selectedLocation = 'All';
  selectedExperience = 'All';
  selectedType = 'All';

  locations = ['All', 'Hyderabad', 'Bangalore', 'Pune', 'Remote', 'Chennai'];
  experienceLevels = ['All', '0-2 Yrs', '2-5 Yrs', '3-5 Yrs', '4-7 Yrs', '5-8 Yrs', '8+ Yrs'];
  jobTypes = ['All', 'Full Time', 'Contract', 'Remote', 'Hybrid'];

  jobs: JobItem[] = [];
  appliedJobIds = new Set<number>();
  applyingJobId: number | null = null;
  isLoading = false;
  isError = false;
  errorMessage = '';

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly jobService: JobService,
    private readonly candidateService: CandidateService,
    private readonly resumeService: ResumeService,
    private readonly applicationService: ApplicationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.isLoading = true;
    this.isError = false;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    forkJoin({
      backendJobs: this.jobService.fetchJobsFromBackend().pipe(
        catchError((err) => {
          console.warn('Could not fetch active jobs from backend, using cached:', err);
          return of(this.jobService.getActiveJobs());
        })
      ),
      myApps: this.applicationService.getMyApplications().pipe(
        catchError((err) => {
          console.warn('Could not fetch candidate applications from backend:', err);
          return of([] as ApplicationResponse[]);
        })
      )
    }).subscribe({
      next: ({ backendJobs, myApps }) => {
        this.isLoading = false;

        // Build set of real job IDs applied by this candidate in MySQL
        this.appliedJobIds = new Set<number>((myApps || []).map(a => a.jobId));

        const baseJobs = (backendJobs && backendJobs.length > 0)
          ? backendJobs.filter(j => j.status === 'Active')
          : this.jobService.getActiveJobs();

        // Get saved job IDs
        const savedIds = this.jobService.getSavedJobIds();

        this.jobs = baseJobs.map(job => ({
          ...job,
          applied: this.appliedJobIds.has(job.id),
          saved: savedIds.includes(job.id)
        }));

        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load jobs or applications:', err);
        this.isLoading = false;
        this.isError = true;
        this.errorMessage = 'Unable to load jobs. Please verify your connection.';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  get filteredJobs(): JobItem[] {
    return this.jobs.filter(job => {
      const matchesSearch = !this.searchQuery ||
        job.title.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        job.company.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        job.tags.some(tag => tag.toLowerCase().includes(this.searchQuery.toLowerCase()));

      const matchesLocation = this.selectedLocation === 'All' || job.location.toLowerCase().includes(this.selectedLocation.toLowerCase());
      const matchesExp = this.selectedExperience === 'All' || job.experience.toLowerCase().includes(this.selectedExperience.toLowerCase());
      const matchesType = this.selectedType === 'All' || job.type.toLowerCase().includes(this.selectedType.toLowerCase());

      return matchesSearch && matchesLocation && matchesExp && matchesType;
    });
  }

  applyToJob(job: JobItem): void {
    if (this.appliedJobIds.has(job.id) || job.applied) {
      alert(`You have already applied for "${job.title}".`);
      return;
    }

    this.applyingJobId = job.id;
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    // 1. Resolve candidate profile
    this.candidateService.getMyProfile().pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (profile) => {
        const candidateId = profile?.id || 0;

        // 2. Resolve resume (optional, backend falls back to latest candidate resume)
        this.resumeService.getMyResume().pipe(
          catchError(() => of(null))
        ).subscribe({
          next: (resume) => {
            const resumeId = resume?.id;

            this.applicationService.applyForJob(candidateId, job.id, resumeId).subscribe({
              next: () => {
                this.applyingJobId = null;
                this.appliedJobIds.add(job.id);
                job.applied = true;
                if (this.selectedJob && this.selectedJob.id === job.id) {
                  this.selectedJob.applied = true;
                }
                this.jobService.applyToJob(job.id);

                this.cdr.markForCheck();
                this.cdr.detectChanges();
                alert(`🎉 Successfully applied for "${job.title}" at ${job.company}! Your application has been submitted to the recruiter.`);
              },
              error: (err) => {
                this.applyingJobId = null;
                if (err?.status === 409 || err?.error?.message?.includes('already applied')) {
                  this.appliedJobIds.add(job.id);
                  job.applied = true;
                  if (this.selectedJob && this.selectedJob.id === job.id) {
                    this.selectedJob.applied = true;
                  }
                  this.jobService.applyToJob(job.id);
                  this.cdr.markForCheck();
                  this.cdr.detectChanges();
                  alert(`You have already applied for "${job.title}".`);
                } else {
                  console.error('Error applying to job on backend:', err);
                  const msg = err?.error?.message || 'Failed to submit application. Please verify your connection.';
                  alert(`⚠️ ${msg}`);
                  this.cdr.markForCheck();
                  this.cdr.detectChanges();
                }
              }
            });
          }
        });
      }
    });
  }

  toggleSave(job: JobItem): void {
    const isSaved = this.jobService.toggleSaveJob(job.id);
    job.saved = isSaved;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  selectedJob: JobItem | null = null;

  openJobDetails(job: JobItem): void {
    this.selectedJob = job;
  }

  closeJobDetails(): void {
    this.selectedJob = null;
  }

  goDashboard(): void {
    this.router.navigate(['/candidate-dashboard']);
  }

  openResume(): void {
    this.router.navigate(['/my-resume']);
  }

  openApplications(): void {
    this.router.navigate(['/my-applications']);
  }

  openInterviews(): void {
    this.router.navigate(['/interviews']);
  }

  openMessages(): void {
    this.router.navigate(['/candidate-messages']);
  }

  contactRecruiter(job?: JobItem): void {
    if (job) {
      this.router.navigate(['/candidate-messages'], {
        queryParams: {
          jobId: job.id,
          jobTitle: job.title,
          company: job.company,
          contactRecruiter: 'true'
        }
      });
    } else {
      this.router.navigate(['/candidate-messages']);
    }
  }

  openProfile(): void {
    this.router.navigate(['/my-profile']);
  }

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}
