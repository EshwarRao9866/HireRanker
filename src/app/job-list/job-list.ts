import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { JobService, JobItem } from '../services/job.service';
import { CandidateService } from '../services/candidate.service';
import { ResumeService } from '../services/resume.service';
import { ApplicationService } from '../services/application.service';

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

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly jobService: JobService,
    private readonly candidateService: CandidateService,
    private readonly resumeService: ResumeService,
    private readonly applicationService: ApplicationService
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.jobs = this.jobService.getActiveJobs();
    this.jobService.fetchJobsFromBackend().subscribe({
      next: (backendJobs) => {
        if (backendJobs && backendJobs.length > 0) {
          this.jobs = backendJobs.filter(j => j.status === 'Active');
        }
      },
      error: () => {}
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
    this.candidateService.getMyProfile().subscribe({
      next: (profile) => {
        const candidateId = profile?.id;
        if (!candidateId) {
          alert('Candidate profile not found. Please log in again.');
          return;
        }

        this.resumeService.getMyResume().subscribe({
          next: (resume) => {
            const resumeId = resume?.id;
            if (!resumeId) {
              alert('Please upload your resume in the My Resume tab before applying.');
              this.router.navigate(['/my-resume']);
              return;
            }

            this.applicationService.applyForJob(candidateId, job.id, resumeId).subscribe({
              next: () => {
                this.jobService.applyToJob(job.id);
                this.loadJobs();
                alert(`🎉 Successfully applied for "${job.title}" at ${job.company}! Your application and resume have been submitted to the recruiter.`);
              },
              error: (err) => {
                if (err?.status === 409 || err?.error?.message?.includes('already applied')) {
                  this.jobService.applyToJob(job.id);
                  this.loadJobs();
                  alert(`You have already applied for "${job.title}".`);
                } else {
                  this.jobService.applyToJob(job.id);
                  this.loadJobs();
                  alert(`🎉 Application for "${job.title}" recorded!`);
                }
              }
            });
          },
          error: () => {
            alert('Please upload your resume in the My Resume tab before applying.');
            this.router.navigate(['/my-resume']);
          }
        });
      },
      error: () => {
        this.jobService.applyToJob(job.id);
        this.loadJobs();
        alert(`🎉 Application for "${job.title}" recorded!`);
      }
    });
  }

  toggleSave(job: JobItem): void {
    this.jobService.toggleSaveJob(job.id);
    this.loadJobs();
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

  openProfile(): void {
    this.router.navigate(['/my-profile']);
  }

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}
