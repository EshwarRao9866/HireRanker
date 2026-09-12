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

  departments = ['All', 'Engineering', 'Product', 'Design', 'Data & AI'];

  newJobType = 'Full Time';
  newJobTags = '';
  newJob = {
    title: '',
    department: 'Engineering',
    location: 'Hyderabad',
    experience: '3-5 Yrs',
    salary: '₹10 - 15 LPA',
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

  saveNewJob(event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.validationError = '';

    if (!this.newJob.title || !this.newJob.title.trim()) {
      this.validationError = 'Job Title is required. Please enter a job title.';
      return;
    }

    const tags = this.newJobTags
      ? this.newJobTags.split(',').map(t => t.trim()).filter(t => t.length > 0)
      : [];

    const created = this.jobService.addJob({
      title: this.newJob.title.trim(),
      department: this.newJob.department || 'Engineering',
      location: this.newJob.location || 'Hyderabad',
      experience: this.newJob.experience || '3-5 Yrs',
      salary: this.newJob.salary || '₹10 - 16 LPA',
      type: this.newJobType || 'Full Time',
      tags: tags,
      status: 'Active'
    });

    this.loadJobs();
    this.newJob = {
      title: '',
      department: 'Engineering',
      location: 'Hyderabad',
      experience: '3-5 Yrs',
      salary: '₹10 - 15 LPA',
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
    this.showEditModal = true;
    this.saveSuccessMessage = '';
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingJob = null;
    this.editJobTags = '';
  }

  saveEditJob(): void {
    if (!this.editingJob || !this.editingJob.title || !this.editingJob.title.trim()) {
      alert('Please enter a valid Job Title');
      return;
    }

    const tags = this.editJobTags
      ? this.editJobTags.split(',').map(t => t.trim()).filter(t => t.length > 0)
      : (this.editingJob.tags || []);

    const updated: JobItem = {
      ...this.editingJob,
      title: this.editingJob.title.trim(),
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
    if (confirm('Are you sure you want to delete this job posting? It will also be removed from the candidate portal.')) {
      this.jobService.deleteJob(id);
      this.loadJobs();
      this.saveSuccessMessage = 'Job posting removed and changes saved.';
      setTimeout(() => (this.saveSuccessMessage = ''), 3000);
    }
  }

  viewApplicants(job: JobItem): void {
    this.router.navigate(['/job-applicants']);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}