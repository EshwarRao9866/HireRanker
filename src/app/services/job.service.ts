import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, map, catchError } from 'rxjs';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';
import { environment } from '../../environments/environment';
import { cleanJobTitle, formatSalaryToLpa } from './salary-formatter.util';

export interface JobItem {
  id: number;
  title: string;
  company: string;
  department: string;
  location: string;
  experience: string;
  type: string;
  salary: string;
  matchScore: number;
  tags: string[];
  description?: string;
  responsibilities?: string;
  applicants: number;
  status: 'Active' | 'Closed' | 'Draft';
  postedDate: string;
  createdDate?: string;
  saved?: boolean;
  applied?: boolean;
}

export interface CandidateApplication {
  id: number;
  jobId: number;
  jobTitle: string;
  company: string;
  candidateName: string;
  candidateEmail: string;
  appliedDate: string;
  matchScore: number;
  status: 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Application Sent';
  location: string;
  salary: string;
}

export interface ApplicantExperience {
  role: string;
  company: string;
  duration: string;
  highlights: string[];
}

export interface ApplicantProject {
  title: string;
  tech: string;
  description: string;
}

export interface ApplicantRecord {
  id: number;
  name: string;
  email: string;
  job: string;
  matchScore: number;
  skillsMatch: number;
  experience: string;
  educationScore: number;
  resumeFileName: string;
  status: 'Shortlisted' | 'Under Review' | 'Rejected' | 'Interview Scheduled';
  phone?: string;
  location?: string;
  github?: string;
  linkedin?: string;
  currentTitle?: string;
  aboutMe?: string;
  skills?: string[];
  education?: string;
  expectedSalary?: string;
  resumeSummary?: string;
  workHistory?: ApplicantExperience[];
  projects?: ApplicantProject[];
}

@Injectable({
  providedIn: 'root'
})
export class JobService {
  private readonly STORAGE_JOBS = 'hireRankerJobs';
  private readonly STORAGE_APPLICATIONS = 'hireRankerApplications';
  private readonly STORAGE_SAVED_JOBS = 'hireRankerSavedJobs';

  readonly jobsSignal = signal<JobItem[]>([]);

  private readonly defaultJobs: JobItem[] = [];

  private readonly defaultApplications: CandidateApplication[] = [];

  private readonly apiUrl = `${environment.apiUrl}/jobs`;

  constructor(
    private readonly authService: AuthService,
    private readonly notificationService: NotificationService,
    private readonly http: HttpClient
  ) {
    this.initJobs();
  }

  private isBrowser(): boolean {
    return typeof window !== 'undefined' && typeof localStorage !== 'undefined';
  }

  private initJobs(): void {
    const jobs = this.getJobs();
    this.jobsSignal.set(jobs);
    if (this.isBrowser()) {
      this.fetchJobsFromBackend().subscribe();
    }
  }

  fetchJobsFromBackend(): Observable<JobItem[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(
      map(backendJobs => {
        if (!backendJobs || !Array.isArray(backendJobs)) {
          return this.getJobs();
        }
        const mapped: JobItem[] = backendJobs.map(bj => ({
          id: bj.id,
          title: cleanJobTitle(bj.title),
          company: bj.company || '',
          department: 'Engineering',
          location: bj.location || 'Hyderabad',
          experience: bj.experienceRequired || '3-5 Yrs',
          type: bj.employmentType === 'FULL_TIME' ? 'Full Time' : bj.employmentType || 'Full Time',
          salary: formatSalaryToLpa(bj.salaryRange),
          matchScore: 92,
          tags: bj.requiredSkills ? bj.requiredSkills.split(',').map((s: string) => s.trim()) : ['Java', 'Spring Boot'],
          description: bj.description || '',
          responsibilities: bj.responsibilities || '',
          applicants: 0,
          status: bj.status === 'ACTIVE' ? 'Active' : bj.status === 'CLOSED' ? 'Closed' : 'Draft',
          postedDate: bj.createdAt ? new Date(bj.createdAt).toLocaleDateString() : 'Recently'
        }));
        if (this.isBrowser()) {
          this.saveJobsToStorage(mapped);
        }
        this.jobsSignal.set(mapped);
        return mapped;
      }),
      catchError(() => of(this.getJobs()))
    );
  }

  getBackendJobs(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getBackendJobById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  createJobBackend(jobData: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, jobData);
  }

  updateJobBackend(id: number, jobData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, jobData);
  }

  deleteJobBackend(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  getJobs(): JobItem[] {
    if (!this.isBrowser()) return [...this.defaultJobs];

    try {
      const raw = localStorage.getItem(this.STORAGE_JOBS);
      let jobs: JobItem[] = [];
      if (raw) {
        const parsed = JSON.parse(raw);
        jobs = Array.isArray(parsed) ? parsed : [];
      }

      // Overlay saved/applied state for current candidate session
      const savedIds = this.getSavedJobIds();
      const appliedIds = this.getAppliedJobIds();

      return jobs.map(j => ({
        ...j,
        createdDate: j.createdDate || j.postedDate || 'Recent',
        saved: savedIds.includes(j.id),
        applied: appliedIds.includes(j.id)
      }));
    } catch (err) {
      console.error('Failed to parse jobs from localStorage:', err);
      return [...this.defaultJobs];
    }
  }

  getActiveJobs(): JobItem[] {
    return this.getJobs().filter(j => j.status === 'Active');
  }

  addJob(jobData: {
    title: string;
    company?: string;
    department?: string;
    location?: string;
    experience?: string;
    salary?: string;
    type?: string;
    tags?: string[];
    description?: string;
    responsibilities?: string;
    status?: 'Active' | 'Closed' | 'Draft';
  }): JobItem {
    const jobs = this.getJobs();

    const company = (jobData.company && jobData.company.trim()) ? jobData.company.trim() : 'HireRanker Technologies';

    // Build tags if not provided
    let tags = jobData.tags && jobData.tags.length > 0 ? jobData.tags : [];
    if (tags.length === 0) {
      const lowerTitle = jobData.title.toLowerCase();
      if (lowerTitle.includes('java')) tags.push('Java', 'Spring Boot', 'SQL');
      if (lowerTitle.includes('angular')) tags.push('Angular', 'TypeScript', 'RxJS');
      if (lowerTitle.includes('react')) tags.push('React', 'JavaScript', 'CSS');
      if (lowerTitle.includes('python') || lowerTitle.includes('ai') || lowerTitle.includes('ml')) tags.push('Python', 'AI/ML', 'FastAPI');
      if (tags.length === 0) tags = ['Full Stack', 'REST APIs', 'Agile'];
    }

    const newJob: JobItem = {
      id: Date.now(),
      title: cleanJobTitle(jobData.title),
      company: company,
      department: jobData.department || 'Engineering',
      location: jobData.location || 'Hyderabad',
      experience: jobData.experience || '2-5 Yrs',
      type: jobData.type || 'Full Time',
      salary: formatSalaryToLpa(jobData.salary),
      matchScore: Math.floor(Math.random() * 15) + 85, // Generates realistic 85-99% match
      tags: tags,
      description: jobData.description || '',
      responsibilities: jobData.responsibilities || '',
      applicants: 0,
      status: jobData.status || 'Active',
      postedDate: 'Just now',
      createdDate: 'Just now',
      saved: false,
      applied: false
    };

    // Prepend so newly posted jobs appear at the top
    const updatedJobs = [newJob, ...jobs];
    this.saveJobsToStorage(updatedJobs);
    this.jobsSignal.set(updatedJobs);

    // Trigger candidate notification for new job opening
    this.notificationService.notifyNewJob(newJob);

    // Sync new job to backend
    this.createJobBackend({
      title: newJob.title,
      company: newJob.company,
      location: newJob.location,
      description: newJob.description,
      responsibilities: newJob.responsibilities,
      requiredSkills: tags.join(', '),
      experienceRequired: newJob.experience,
      salaryRange: newJob.salary,
      employmentType: 'FULL_TIME',
      status: newJob.status === 'Closed' ? 'CLOSED' : 'ACTIVE'
    }).subscribe({
      next: (created) => {
        if (created && created.id) {
          newJob.id = created.id;
          this.saveJobsToStorage(this.jobsSignal());
        }
      },
      error: () => {}
    });

    return newJob;
  }

  updateJob(updatedJob: JobItem): boolean {
    const jobs = this.getJobs();
    const index = jobs.findIndex(j => j.id === updatedJob.id);
    if (index !== -1) {
      jobs[index] = {
        ...jobs[index],
        title: cleanJobTitle(updatedJob.title),
        company: updatedJob.company,
        department: updatedJob.department,
        location: updatedJob.location,
        experience: updatedJob.experience,
        salary: formatSalaryToLpa(updatedJob.salary),
        type: updatedJob.type,
        tags: updatedJob.tags,
        description: updatedJob.description,
        responsibilities: updatedJob.responsibilities,
        status: updatedJob.status
      };
      this.saveJobsToStorage(jobs);
      this.jobsSignal.set([...jobs]);

      this.updateJobBackend(updatedJob.id, {
        title: cleanJobTitle(updatedJob.title),
        company: updatedJob.company,
        location: updatedJob.location,
        description: updatedJob.description,
        responsibilities: updatedJob.responsibilities,
        requiredSkills: updatedJob.tags ? updatedJob.tags.join(', ') : 'Java, Angular',
        experienceRequired: updatedJob.experience,
        salaryRange: formatSalaryToLpa(updatedJob.salary),
        employmentType: 'FULL_TIME',
        status: updatedJob.status === 'Closed' ? 'CLOSED' : 'ACTIVE'
      }).subscribe({ error: () => {} });

      return true;
    }
    return false;
  }

  updateJobStatus(id: number, status: 'Active' | 'Closed'): void {
    const jobs = this.getJobs();
    const index = jobs.findIndex(j => j.id === id);
    if (index !== -1) {
      jobs[index].status = status;
      this.saveJobsToStorage(jobs);
      this.jobsSignal.set([...jobs]);

      this.updateJobBackend(id, {
        status: status === 'Active' ? 'ACTIVE' : 'CLOSED'
      }).subscribe({ error: () => {} });
    }
  }

  deleteJob(id: number): void {
    let jobs = this.getJobs();
    jobs = jobs.filter(j => j.id !== id);
    this.saveJobsToStorage(jobs);
    this.jobsSignal.set(jobs);

    this.deleteJobBackend(id).subscribe({ error: () => {} });
  }

  applyToJob(jobId: number, candidateName?: string, candidateEmail?: string): boolean {
    const jobs = this.getJobs();
    const job = jobs.find(j => j.id === jobId);
    if (!job) return false;

    // Check if already applied
    const appliedIds = this.getAppliedJobIds();
    if (appliedIds.includes(jobId)) return true;

    // Increment applicants on the job
    job.applicants = (job.applicants || 0) + 1;
    job.applied = true;
    this.saveJobsToStorage(jobs);

    // Save applied ID
    appliedIds.push(jobId);

    // Retrieve active candidate info
    const profile = this.authService.getCandidateProfile();
    const user = this.authService.currentUser();
    const name = candidateName || profile?.fullName || user?.fullName || 'Eshwar Rao';
    const email = candidateEmail || profile?.email || user?.email || 'eshwar@candidate.com';

    // Add to Candidate Applications
    const applications = this.getCandidateApplications();
    const newApp: CandidateApplication = {
      id: Date.now(),
      jobId: job.id,
      jobTitle: cleanJobTitle(job.title),
      company: job.company,
      candidateName: name,
      candidateEmail: email,
      appliedDate: 'Just now',
      matchScore: job.matchScore || 90,
      status: 'Application Sent',
      location: job.location,
      salary: formatSalaryToLpa(job.salary)
    };

    applications.unshift(newApp);
    this.saveCandidateApplications(applications);
    this.jobsSignal.set([...jobs]);

    // Trigger admin notification for new application
    this.notificationService.notifyNewApplication({
      candidateName: name,
      candidateEmail: email,
      jobTitle: job.title,
      jobId: job.id,
      matchScore: job.matchScore || 90
    });

    return true;
  }

  toggleSaveJob(jobId: number): boolean {
    const savedIds = this.getSavedJobIds();
    const index = savedIds.indexOf(jobId);
    let isSaved: boolean;

    if (index !== -1) {
      savedIds.splice(index, 1);
      isSaved = false;
    } else {
      savedIds.push(jobId);
      isSaved = true;
    }

    this.saveSavedJobIds(savedIds);

    const jobs = this.getJobs();
    this.jobsSignal.set(jobs);
    return isSaved;
  }

  getCandidateApplications(): CandidateApplication[] {
    if (!this.isBrowser()) return [];

    const raw = localStorage.getItem(this.STORAGE_APPLICATIONS);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
      } catch {
        return [];
      }
    }

    return [];
  }

  saveCandidateApplications(apps: CandidateApplication[]): void {
    if (!this.isBrowser()) return;
    localStorage.setItem(this.STORAGE_APPLICATIONS, JSON.stringify(apps));
  }

  getAppliedJobsCount(): number {
    return this.getCandidateApplications().length;
  }

  getApplicants(): ApplicantRecord[] {
    const savedProfile = this.authService.getCandidateProfile();
    const savedSkills = this.authService.getCandidateSkills();

    const statusMapRaw = this.isBrowser() ? localStorage.getItem('hireRankerApplicantStatuses') : null;
    let statusMap: Record<string, 'Shortlisted' | 'Under Review' | 'Rejected' | 'Interview Scheduled'> = {};
    if (statusMapRaw) {
      try { statusMap = JSON.parse(statusMapRaw); } catch {}
    }

    const baseApplicants: ApplicantRecord[] = [];

    // Include dynamically applied candidate applications
    const apps = this.getCandidateApplications();
    let maxId = baseApplicants.reduce((max, b) => Math.max(max, b.id || 0), 10);

    for (const app of apps) {
      if (!baseApplicants.some(b => b.job === app.jobTitle && b.email === app.candidateEmail)) {
        const isCandidateSelf = (savedProfile && (app.candidateEmail === savedProfile.email || app.candidateName.toLowerCase() === savedProfile.fullName.toLowerCase())) ||
          app.candidateEmail === 'eshwar@candidate.com' ||
          app.candidateEmail.includes('centurionuniv.edu.in');

        const applicantPhone = isCandidateSelf ? (savedProfile?.mobile || '') : '';
        const applicantLocation = isCandidateSelf ? (savedProfile?.location || app.location || '') : (app.location || '');
        const applicantGithub = isCandidateSelf ? (savedProfile?.gitHub || '') : '';
        const applicantLinkedin = isCandidateSelf ? (savedProfile?.linkedIn || '') : '';
        const applicantTitle = isCandidateSelf ? (savedProfile?.currentTitle || app.jobTitle) : app.jobTitle;
        const applicantAbout = isCandidateSelf ? (savedProfile?.aboutMe || '') : '';
        const applicantSkills = isCandidateSelf && savedSkills && savedSkills.length > 0 ? savedSkills : [];
        const applicantEducation = isCandidateSelf ? (savedProfile?.education || '') : '';

        const statusKey = `${app.candidateEmail}_${app.jobTitle}`;
        const finalStatus = statusMap[statusKey] || (app.status as any) || 'Under Review';

        // Assign strictly unique ID to eliminate NG0955 duplicate track keys
        const uniqueId = baseApplicants.some(b => b.id === app.id) ? ++maxId : app.id;

        baseApplicants.unshift({
          id: uniqueId,
          name: app.candidateName,
          email: app.candidateEmail,
          job: app.jobTitle,
          matchScore: app.matchScore,
          skillsMatch: Math.min(100, app.matchScore + 2),
          experience: '',
          educationScore: 0,
          resumeFileName: `${app.candidateName.replace(/\s+/g, '_')}_Resume.pdf`,
          status: finalStatus,
          phone: applicantPhone,
          location: applicantLocation,
          github: applicantGithub,
          linkedin: applicantLinkedin,
          currentTitle: applicantTitle,
          aboutMe: applicantAbout,
          skills: applicantSkills,
          education: applicantEducation,
          expectedSalary: app.salary || '',
          workHistory: [],
          projects: []
        });
      }
    }

    return baseApplicants;
  }

  updateApplicantStatus(applicantId: number, email: string, jobTitle: string, newStatus: 'Shortlisted' | 'Under Review' | 'Rejected' | 'Interview Scheduled'): void {
    if (!this.isBrowser()) return;

    // 1. Update candidate applications in localStorage
    const apps = this.getCandidateApplications();
    let updated = false;
    for (const app of apps) {
      if ((app.id === applicantId || app.candidateEmail === email) && app.jobTitle === jobTitle) {
        app.status = newStatus as any;
        updated = true;
      }
    }
    if (updated) {
      this.saveCandidateApplications(apps);
    }

    // 2. Persist in applicant status map
    const statusMapRaw = localStorage.getItem('hireRankerApplicantStatuses');
    let statusMap: Record<string, string> = {};
    if (statusMapRaw) {
      try { statusMap = JSON.parse(statusMapRaw); } catch {}
    }
    statusMap[`${email}_${jobTitle}`] = newStatus;
    localStorage.setItem('hireRankerApplicantStatuses', JSON.stringify(statusMap));
  }

  private getSavedJobIds(): number[] {
    if (!this.isBrowser()) return [];
    const raw = localStorage.getItem(this.STORAGE_SAVED_JOBS);
    if (raw) {
      try { return JSON.parse(raw); } catch { return []; }
    }
    return [];
  }

  private saveSavedJobIds(ids: number[]): void {
    if (!this.isBrowser()) return;
    localStorage.setItem(this.STORAGE_SAVED_JOBS, JSON.stringify(ids));
  }

  private getAppliedJobIds(): number[] {
    const apps = this.getCandidateApplications();
    return apps.map(a => a.jobId);
  }

  private saveJobsToStorage(jobs: JobItem[]): void {
    if (!this.isBrowser()) return;
    try {
      // Strip ephemeral candidate-specific flags before persisting jobs array
      const cleanJobs = jobs.map(({ saved, applied, ...rest }) => rest);
      localStorage.setItem(this.STORAGE_JOBS, JSON.stringify(cleanJobs));
    } catch (err) {
      console.error('Failed to save jobs to localStorage:', err);
    }
  }
}
