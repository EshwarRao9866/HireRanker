import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { JobService, JobItem } from '../services/job.service';

@Component({
  selector: 'app-job-postings',
  standalone: true,
  templateUrl: './job-postings.html',
  imports: [CommonModule, FormsModule],
  styleUrl: './job-postings.css'
})
export class JobPostings implements OnInit {
  searchText = '';
  selectedDepartment = 'All';
  showCreateModal = false;
  validationError = '';
  saveSuccessMessage = '';

  departments = ['All', 'Engineering', 'Software Development', 'Product', 'Design', 'Data & AI', 'Human Resources', 'Finance', 'Marketing', 'Sales', 'Operations'];

  newJobType = 'Full Time';
  newJobTags = '';
  newJob = {
    company: '',
    title: '',
    department: 'Engineering',
    location: '',
    experience: '',
    salary: '',
    description: '',
    responsibilities: '',
    status: 'Active' as 'Active' | 'Closed' | 'Draft'
  };

  jobs: JobItem[] = [];

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly jobService: JobService
  ) {}

  ngOnInit(): void {
    this.loadJobs();
    if (this.route.snapshot.queryParams['create'] === 'true') {
      this.openCreateModal();
    }
  }

  loadJobs(): void {
    this.jobs = this.jobService.getJobs();
    this.jobService.fetchJobsFromBackend().subscribe({
      next: (backendJobs) => {
        if (backendJobs) {
          this.jobs = backendJobs;
        }
      }
    });
  }

  get activeJobsCount(): number {
    return this.jobs.filter(j => j.status === 'Active').length;
  }

  get totalApplicantsCount(): number {
    return this.jobs.reduce((sum, j) => sum + (j.applicants || 0), 0);
  }

  get filteredJobs(): JobItem[] {
    return this.jobs.filter(j => {
      const matchesSearch = !this.searchText ||
        j.title.toLowerCase().includes(this.searchText.toLowerCase()) ||
        j.company.toLowerCase().includes(this.searchText.toLowerCase()) ||
        j.location.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesDept = this.selectedDepartment === 'All' || j.department === this.selectedDepartment;

      return matchesSearch && matchesDept;
    });
  }

  openCreateModal(): void {
    this.showCreateModal = true;
    this.validationError = '';
    this.saveSuccessMessage = '';
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.validationError = '';
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  showEditModal = false;
  editingJob: JobItem | null = null;
  editJobTags = '';
  editValidationError = '';

  saveNewJob(event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.validationError = '';

    if (!this.newJob.company || !this.newJob.company.trim()) {
      this.validationError = 'Company Name is required.';
      return;
    }
    if (!this.newJob.title || !this.newJob.title.trim()) {
      this.validationError = 'Job Title is required.';
      return;
    }
    if (!this.newJob.department || !this.newJob.department.trim()) {
      this.validationError = 'Department is required.';
      return;
    }
    if (!this.newJob.location || !this.newJob.location.trim()) {
      this.validationError = 'Location is required.';
      return;
    }
    if (!this.newJob.experience || !this.newJob.experience.trim()) {
      this.validationError = 'Experience Required is required.';
      return;
    }
    if (!this.newJobTags || !this.newJobTags.trim()) {
      this.validationError = 'Key Skills / Tags are required.';
      return;
    }
    if (!this.newJob.description || !this.newJob.description.trim()) {
      this.validationError = 'Job Description is required.';
      return;
    }
    if (!this.newJob.responsibilities || !this.newJob.responsibilities.trim()) {
      this.validationError = 'Responsibilities are required.';
      return;
    }

    const tags = this.newJobTags
      ? this.newJobTags.split(',').map(t => t.trim()).filter(t => t.length > 0)
      : [];

    const created = this.jobService.addJob({
      company: this.newJob.company.trim(),
      title: this.newJob.title.trim(),
      department: this.newJob.department.trim(),
      location: this.newJob.location.trim(),
      experience: this.newJob.experience.trim(),
      salary: this.newJob.salary ? this.newJob.salary.trim() : '',
      type: this.newJobType || 'Full Time',
      tags: tags,
      description: this.newJob.description.trim(),
      responsibilities: this.newJob.responsibilities.trim(),
      status: 'Active'
    });

    this.loadJobs();
    this.newJob = {
      company: '',
      title: '',
      department: 'Engineering',
      location: '',
      experience: '',
      salary: '',
      description: '',
      responsibilities: '',
      status: 'Active'
    };
    this.newJobTags = '';
    this.showCreateModal = false;
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });

    this.saveSuccessMessage = `🎉 Job opening "${created.title}" published and saved successfully! Candidates have been notified.`;
    setTimeout(() => (this.saveSuccessMessage = ''), 5000);
  }

  openEditModal(job: JobItem): void {
    this.editingJob = { ...job };
    this.editJobTags = job.tags ? job.tags.join(', ') : '';
    this.editValidationError = '';
    this.showEditModal = true;
    this.saveSuccessMessage = '';
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingJob = null;
    this.editJobTags = '';
    this.editValidationError = '';
  }

  saveEditJob(): void {
    if (!this.editingJob) return;

    this.editValidationError = '';
    if (!this.editingJob.company || !this.editingJob.company.trim()) {
      this.editValidationError = 'Company Name is required.';
      return;
    }
    if (!this.editingJob.title || !this.editingJob.title.trim()) {
      this.editValidationError = 'Job Title is required.';
      return;
    }
    if (!this.editingJob.department || !this.editingJob.department.trim()) {
      this.editValidationError = 'Department is required.';
      return;
    }
    if (!this.editingJob.location || !this.editingJob.location.trim()) {
      this.editValidationError = 'Location is required.';
      return;
    }
    if (!this.editingJob.experience || !this.editingJob.experience.trim()) {
      this.editValidationError = 'Experience Required is required.';
      return;
    }
    if (!this.editJobTags || !this.editJobTags.trim()) {
      this.editValidationError = 'Key Skills / Tags are required.';
      return;
    }
    if (!this.editingJob.description || !this.editingJob.description.trim()) {
      this.editValidationError = 'Job Description is required.';
      return;
    }
    if (!this.editingJob.responsibilities || !this.editingJob.responsibilities.trim()) {
      this.editValidationError = 'Responsibilities are required.';
      return;
    }

    const tags = this.editJobTags
      ? this.editJobTags.split(',').map(t => t.trim()).filter(t => t.length > 0)
      : (this.editingJob.tags || []);

    const updated: JobItem = {
      ...this.editingJob,
      company: this.editingJob.company.trim(),
      title: this.editingJob.title.trim(),
      department: this.editingJob.department.trim(),
      location: this.editingJob.location.trim(),
      experience: this.editingJob.experience.trim(),
      salary: this.editingJob.salary ? this.editingJob.salary.trim() : '',
      description: this.editingJob.description.trim(),
      responsibilities: this.editingJob.responsibilities.trim(),
      tags: tags
    };

    const success = this.jobService.updateJob(updated);
    if (success) {
      this.loadJobs();
      this.closeEditModal();
      this.saveSuccessMessage = `Job posting "${updated.title}" updated and saved successfully!`;
      setTimeout(() => (this.saveSuccessMessage = ''), 4000);
    }
  }

  toggleJobStatus(job: JobItem): void {
    const nextStatus = job.status === 'Active' ? 'Closed' : 'Active';
    this.jobService.updateJobStatus(job.id, nextStatus);
    this.loadJobs();
    this.saveSuccessMessage = `Job status updated to ${nextStatus} and saved!`;
    setTimeout(() => (this.saveSuccessMessage = ''), 3000);
  }

  deleteJob(id: number): void {
    if (confirm('Are you sure you want to delete this job posting? It will also be removed from the candidate portal and MySQL database.')) {
      this.jobService.deleteJob(id).subscribe({
        next: () => {
          this.loadJobs();
          this.saveSuccessMessage = 'Job posting removed and changes saved.';
          setTimeout(() => (this.saveSuccessMessage = ''), 3000);
        },
        error: (err) => {
          console.error('Failed to delete job:', err);
          this.saveSuccessMessage = 'Failed to delete job. Please try again.';
          setTimeout(() => (this.saveSuccessMessage = ''), 4000);
        }
      });
    }
  }

  viewApplicants(job: JobItem): void {
    this.router.navigate(['/job-applicants']);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}