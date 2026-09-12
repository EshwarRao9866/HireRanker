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

  private readonly defaultJobs: JobItem[] = [
    {
      id: 1,
      title: 'Java Full Stack Developer',
      company: 'HireRanker Technologies',
      department: 'Engineering',
      location: 'Hyderabad',
      experience: '3-5 Yrs',
      type: 'Full Time',
      salary: '₹10 - 16 LPA',
      matchScore: 94,
      tags: ['Java', 'Spring Boot', 'Angular', 'Microservices', 'SQL'],
      applicants: 42,
      status: 'Active',
      postedDate: '2 days ago',
      createdDate: new Date(Date.now() - 6 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    },
    {
      id: 2,
      title: 'Senior Angular Developer',
      company: 'HireRanker Technologies',
      department: 'Engineering',
      location: 'Bangalore',
      experience: '4-7 Yrs',
      type: 'Full Time',
      salary: '₹14 - 20 LPA',
      matchScore: 89,
      tags: ['Angular 18+', 'TypeScript', 'RxJS', 'NgRx', 'Tailwind'],
      applicants: 31,
      status: 'Active',
      postedDate: '1 day ago',
      createdDate: new Date(Date.now() - 4 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    },
    {
      id: 3,
      title: 'UI/UX Product Designer',
      company: 'HireRanker Technologies',
      department: 'Design',
      location: 'Remote',
      experience: '3-5 Yrs',
      type: 'Remote',
      salary: '₹8 - 14 LPA',
      matchScore: 82,
      tags: ['Figma', 'Design Systems', 'CSS Grid', 'Wireframing'],
      applicants: 18,
      status: 'Closed',
      postedDate: '10 days ago',
      createdDate: new Date(Date.now() - 10 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    },
    {
      id: 4,
      title: 'AI/ML Resume Screening Engineer',
      company: 'HireRanker Technologies',
      department: 'Data & AI',
      location: 'Hyderabad / Hybrid',
      experience: '2-5 Yrs',
      type: 'Full Time',
      salary: '₹12 - 18 LPA',
      matchScore: 92,
      tags: ['Python', 'TensorFlow', 'NLP', 'FastAPI', 'Docker'],
      applicants: 29,
      status: 'Active',
      postedDate: 'Just now',
      createdDate: new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    },
    {
      id: 5,
      title: 'Cloud & AI Software Engineer',
      company: 'Innovate Systems Ltd',
      department: 'Engineering',
      location: 'Remote',
      experience: '5-8 Yrs',
      type: 'Remote',
      salary: '₹14 - 22 LPA',
      matchScore: 86,
      tags: ['Python', 'Java', 'AWS', 'OpenAI API', 'Docker'],
      applicants: 25,
      status: 'Active',
      postedDate: '3 days ago',
      createdDate: new Date(Date.now() - 3 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    },
    {
      id: 6,
      title: 'Backend Java Developer',
      company: 'FinTech Nexus',
      department: 'Engineering',
      location: 'Pune',
      experience: '3-5 Yrs',
      type: 'Full Time',
      salary: '₹9 - 15 LPA',
      matchScore: 83,
      tags: ['Java 21', 'Spring Cloud', 'Kafka', 'PostgreSQL'],
      applicants: 19,
      status: 'Active',
      postedDate: '4 days ago',
      createdDate: new Date(Date.now() - 4 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    },
    {
      id: 7,
      title: 'Web Developer',
      company: 'HireRanker Technologies',
      department: 'Engineering',
      location: 'Hyderabad / Remote',
      experience: '2-4 Yrs',
      type: 'Full Time',
      salary: '₹8 - 14 LPA',
      matchScore: 91,
      tags: ['HTML5', 'CSS3', 'JavaScript', 'TypeScript', 'Angular', 'Bootstrap'],
      applicants: 28,
      status: 'Active',
      postedDate: '1 day ago',
      createdDate: new Date(Date.now() - 1 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    }
  ];

  private readonly defaultApplications: CandidateApplication[] = [
    {
      id: 1,
      jobId: 1,
      jobTitle: 'Java Full Stack Developer',
      company: 'HireRanker Technologies',
      candidateName: 'Eshwar Rao',
      candidateEmail: 'eshwar@candidate.com',
      appliedDate: new Date(Date.now() - 4 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
      matchScore: 94,
      status: 'Shortlisted',
      location: 'Hyderabad',
      salary: '₹10 - 16 LPA'
    },
    {
      id: 2,
      jobId: 2,
      jobTitle: 'Senior Angular Developer',
      company: 'HireRanker Technologies',
      candidateName: 'Eshwar Rao',
      candidateEmail: 'eshwar@candidate.com',
      appliedDate: new Date(Date.now() - 2 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
      matchScore: 89,
      status: 'Interview Scheduled',
      location: 'Bangalore',
      salary: '₹14 - 20 LPA'
    },
    {
      id: 3,
      jobId: 5,
      jobTitle: 'Cloud & AI Software Engineer',
      company: 'Innovate Systems Ltd',
      candidateName: 'Eshwar Rao',
      candidateEmail: 'eshwar@candidate.com',
      appliedDate: new Date(Date.now() - 1 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
      matchScore: 86,
      status: 'Under Review',
      location: 'Remote',
      salary: '₹14 - 22 LPA'
    },
    {
      id: 4,
      jobId: 7,
      jobTitle: 'Web Developer',
      company: 'HireRanker Technologies',
      candidateName: 'Suresh Kumar',
      candidateEmail: 'suresh.kumar@email.com',
      appliedDate: new Date(Date.now() - 1 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
      matchScore: 91,
      status: 'Shortlisted',
      location: 'Hyderabad',
      salary: '₹8 - 14 LPA'
    }
  ];

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
        if (!backendJobs || !Array.isArray(backendJobs) || backendJobs.length === 0) {
          return this.getJobs();
        }
        const mapped: JobItem[] = backendJobs.map(bj => ({
          id: bj.id,
          title: cleanJobTitle(bj.title),
          company: bj.company || 'HireRanker Technologies',
          department: 'Engineering',
          location: bj.location || 'Hyderabad',
          experience: bj.experienceRequired || '3-5 Yrs',
          type: bj.employmentType === 'FULL_TIME' ? 'Full Time' : bj.employmentType || 'Full Time',
          salary: formatSalaryToLpa(bj.salaryRange),
          matchScore: 92,
          tags: bj.requiredSkills ? bj.requiredSkills.split(',').map((s: string) => s.trim()) : ['Java', 'Spring Boot'],
          description: bj.description,
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
      let jobs: JobItem[];
      if (raw) {
        const parsed = JSON.parse(raw);
        jobs = Array.isArray(parsed) && parsed.length > 0 ? parsed : [...this.defaultJobs];
      } else {
        jobs = [...this.defaultJobs];
        this.saveJobsToStorage(jobs);
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
    department?: string;
    location?: string;
    experience?: string;
    salary?: string;
    type?: string;
    tags?: string[];
    company?: string;
    status?: 'Active' | 'Closed' | 'Draft';
  }): JobItem {
    const jobs = this.getJobs();

    // Default company from Admin Settings if available
    const adminSettings = this.authService.getAdminSettings();
    const company = jobData.company || adminSettings?.companyName || 'HireRanker Technologies';

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
      description: newJob.title + ' role at ' + newJob.company,
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
        department: updatedJob.department,
        location: updatedJob.location,
        experience: updatedJob.experience,
        salary: formatSalaryToLpa(updatedJob.salary),
        type: updatedJob.type,
        tags: updatedJob.tags,
        status: updatedJob.status
      };
      this.saveJobsToStorage(jobs);
      this.jobsSignal.set([...jobs]);

      this.updateJobBackend(updatedJob.id, {
        title: cleanJobTitle(updatedJob.title),
        company: updatedJob.company,
        location: updatedJob.location,
        description: updatedJob.description || (updatedJob.title + ' position'),
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
    if (!this.isBrowser()) return [...this.defaultApplications];

    const raw = localStorage.getItem(this.STORAGE_APPLICATIONS);
    if (raw) {
      try {
        return JSON.parse(raw);
      } catch {
        return [...this.defaultApplications];
      }
    }

    localStorage.setItem(this.STORAGE_APPLICATIONS, JSON.stringify(this.defaultApplications));
    return [...this.defaultApplications];
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

    const baseApplicants: ApplicantRecord[] = [
      {
        id: 1,
        name: 'Eshwar Rao',
        email: 'eshwar.rao@email.com',
        job: 'Java Full Stack Developer',
        matchScore: 95,
        skillsMatch: 96,
        experience: '4.5 yrs',
        educationScore: 92,
        resumeFileName: 'Eshwar_Rao_Resume.pdf',
        status: statusMap['eshwar.rao@email.com_Java Full Stack Developer'] || 'Shortlisted',
        phone: savedProfile?.mobile || '+91 98765 43210',
        location: savedProfile?.location || 'Hyderabad, Telangana, India',
        github: savedProfile?.gitHub || 'https://github.com/eshwar-rao',
        linkedin: savedProfile?.linkedIn || 'https://linkedin.com/in/eshwar-rao',
        currentTitle: savedProfile?.currentTitle || 'Senior Full Stack Java & Angular Developer',
        aboutMe: savedProfile?.aboutMe || 'Passionate Full Stack Engineer with 4.5+ years of experience designing high-performance web applications using Angular, TypeScript, Java, and Spring Boot. Strong track record in developing RESTful microservices, AI-driven recruitment platforms, and responsive user interfaces with top-tier accessibility standards.',
        skills: savedSkills && savedSkills.length > 0 ? savedSkills : ['Java', 'Spring Boot', 'Angular', 'TypeScript', 'Microservices', 'SQL', 'Docker', 'REST APIs', 'Git', 'HTML5/CSS3'],
        education: 'B.Tech in Computer Science & Engineering - Centurion University (CGPA: 8.9 / 10)',
        expectedSalary: savedProfile?.expectedSalary || '₹14 - 18 LPA',
        workHistory: [
          {
            role: 'Senior Full Stack Developer',
            company: 'TechGlobal Solutions',
            duration: '2024 - Present (1.5 yrs)',
            highlights: [
              'Architected Angular 18 enterprise frontends with reactive state management and standalone components.',
              'Engineered Spring Boot microservices processing 25,000+ recruitment events daily with 99.9% uptime.',
              'Spearheaded ATS resume parsing algorithm integration improving candidate-to-job matching accuracy by 34%.'
            ]
          },
          {
            role: 'Full Stack Software Engineer',
            company: 'CloudNova Systems',
            duration: '2022 - 2024 (2 yrs)',
            highlights: [
              'Developed secure RESTful APIs with Spring Security, JWT authentication, and PostgreSQL.',
              'Optimized database queries and Redis caching, cutting median endpoint response times by 45%.'
            ]
          }
        ],
        projects: [
          {
            title: 'HireRanker AI Recruitment Suite',
            tech: 'Angular 18, Spring Boot 3, PostgreSQL, Docker',
            description: 'Automated recruitment screening platform featuring intelligent resume parsing, matching algorithms, and role-based workflows.'
          },
          {
            title: 'Enterprise Analytics Microservice',
            tech: 'Java 21, Spring Cloud, Kafka, TypeScript',
            description: 'High-throughput metric collection and live dashboard for recruitment funnels and candidate status tracking.'
          }
        ]
      },
      {
        id: 2,
        name: 'Krupa Jyothi',
        email: 'krupa.jyothi@email.com',
        job: 'Senior Angular Developer',
        matchScore: 89,
        skillsMatch: 92,
        experience: '3.8 yrs',
        educationScore: 88,
        resumeFileName: 'Krupa_Jyothi_Angular.pdf',
        status: statusMap['krupa.jyothi@email.com_Senior Angular Developer'] || 'Shortlisted',
        phone: '+91 98450 11234',
        location: 'Bangalore, Karnataka, India',
        github: 'https://github.com/krupa-jyothi',
        linkedin: 'https://linkedin.com/in/krupa-jyothi',
        currentTitle: 'Senior Frontend & Angular Developer',
        aboutMe: 'Frontend architect with 3.8+ years building enterprise Angular SPAs, custom UI component libraries, and high-performance reactive applications with RxJS and NgRx.',
        skills: ['Angular 18', 'TypeScript', 'RxJS', 'NgRx', 'Tailwind CSS', 'SCSS', 'HTML5', 'Jest', 'Git', 'Webpack'],
        education: 'B.Tech in Information Technology - JNTU Hyderabad (2018 - 2022)',
        expectedSalary: '₹14 - 20 LPA',
        workHistory: [
          {
            role: 'Senior Angular Developer',
            company: 'Apex Fintech Solutions',
            duration: '2023 - Present (2 yrs)',
            highlights: [
              'Designed responsive trader dashboards using Angular signals and reactive forms.',
              'Reduced client bundle size by 38% using lazy loading and build optimization.'
            ]
          }
        ],
        projects: [
          {
            title: 'Financial Analytics Portal',
            tech: 'Angular, RxJS, NgRx, Chart.js',
            description: 'Real-time asset tracking and investment insights portal.'
          }
        ]
      },
      {
        id: 3,
        name: 'Durga Rohith',
        email: 'durga.rohith@email.com',
        job: 'AI/ML Resume Screening Engineer',
        matchScore: 84,
        skillsMatch: 86,
        experience: '3.2 yrs',
        educationScore: 85,
        resumeFileName: 'Durga_Rohith_Resume.pdf',
        status: statusMap['durga.rohith@email.com_AI/ML Resume Screening Engineer'] || 'Under Review',
        phone: '+91 97123 45678',
        location: 'Hyderabad, Telangana, India',
        github: 'https://github.com/durga-rohith',
        linkedin: 'https://linkedin.com/in/durga-rohith',
        currentTitle: 'AI/ML Engineer & NLP Specialist',
        aboutMe: 'Machine Learning engineer specializing in NLP, resume screening pipelines, Python FastAPI microservices, and transformer embeddings.',
        skills: ['Python', 'FastAPI', 'PyTorch', 'HuggingFace', 'Docker', 'NLP', 'PostgreSQL', 'Scikit-Learn', 'Git'],
        education: 'M.Tech in Artificial Intelligence & Data Science - Osmania University (2020 - 2022)',
        expectedSalary: '₹12 - 18 LPA',
        workHistory: [
          {
            role: 'ML Engineer',
            company: 'CogniHire Labs',
            duration: '2022 - Present (2.5 yrs)',
            highlights: [
              'Fine-tuned transformer models for resume skill extraction and semantic relevance ranking.'
            ]
          }
        ],
        projects: [
          {
            title: 'Resume Semantic Extractor',
            tech: 'Python, FastAPI, BERT, Docker',
            description: 'Automated resume parser matching candidate profiles against job descriptions.'
          }
        ]
      },
      {
        id: 4,
        name: 'Suresh Kumar',
        email: 'suresh.kumar@email.com',
        job: 'Web Developer',
        matchScore: 91,
        skillsMatch: 93,
        experience: '3 yrs',
        educationScore: 89,
        resumeFileName: 'Suresh_Kumar_Resume.pdf',
        status: statusMap['suresh.kumar@email.com_Web Developer'] || 'Shortlisted',
        phone: '+91 98111 22334',
        location: 'Hyderabad, India',
        github: 'https://github.com/suresh-kumar',
        linkedin: 'https://linkedin.com/in/suresh-kumar',
        currentTitle: 'Frontend & Web Developer',
        aboutMe: 'Responsive web developer experienced in HTML5, CSS3, JavaScript, TypeScript, and modern Angular/React frameworks.',
        skills: ['HTML5', 'CSS3', 'JavaScript', 'TypeScript', 'Angular', 'Bootstrap', 'REST APIs', 'Git'],
        education: 'B.Tech in CSE - Osmania University',
        expectedSalary: '₹8 - 14 LPA',
        workHistory: [
          {
            role: 'Web Developer',
            company: 'WebCraft Studios',
            duration: '2022 - Present (2.5 yrs)',
            highlights: ['Built responsive websites and enterprise portals using HTML5, CSS3, and JavaScript/Angular.']
          }
        ],
        projects: [
          {
            title: 'Modern Web Application Platform',
            tech: 'HTML5, CSS3, Angular, REST',
            description: 'Customer portal with dynamic dashboards and responsive layouts.'
          }
        ]
      },
      {
        id: 5,
        name: 'Sowmya Maloth',
        email: 'sowmya.maloth@email.com',
        job: 'UI/UX Product Designer',
        matchScore: 88,
        skillsMatch: 90,
        experience: '3.5 yrs',
        educationScore: 86,
        resumeFileName: 'Sowmya_Maloth_Resume.pdf',
        status: statusMap['sowmya.maloth@email.com_UI/UX Product Designer'] || 'Under Review',
        phone: '+91 99887 66554',
        location: 'Hyderabad, India',
        github: 'https://github.com/sowmya-maloth',
        linkedin: 'https://linkedin.com/in/sowmya-maloth',
        currentTitle: 'Lead Product & UI/UX Designer',
        aboutMe: 'Product designer with 3.5+ years experience building accessible design systems, user journey maps, and high-fidelity prototypes in Figma.',
        skills: ['Figma', 'Design Systems', 'CSS Grid', 'Wireframing', 'User Research', 'Prototyping', 'Accessibility'],
        education: 'B.Des in Interaction Design - NID (2019 - 2023)',
        expectedSalary: '₹10 - 15 LPA',
        workHistory: [
          {
            role: 'Senior Product Designer',
            company: 'Studio Pixel',
            duration: '2023 - Present (1.5 yrs)',
            highlights: ['Built comprehensive design token library used across web and mobile platforms.']
          }
        ],
        projects: [
          {
            title: 'Enterprise Design System',
            tech: 'Figma, Storybook, CSS3',
            description: 'Scalable UI components and style guide for enterprise SaaS applications.'
          }
        ]
      },
      {
        id: 6,
        name: 'Bala Vardhan',
        email: 'bala.vardhan@email.com',
        job: 'Cloud & AI Software Engineer',
        matchScore: 86,
        skillsMatch: 88,
        experience: '4 yrs',
        educationScore: 90,
        resumeFileName: 'Bala_Vardhan_Resume.pdf',
        status: statusMap['bala.vardhan@email.com_Cloud & AI Software Engineer'] || 'Shortlisted',
        phone: '+91 98711 33445',
        location: 'Bangalore, India',
        github: 'https://github.com/bala-vardhan',
        linkedin: 'https://linkedin.com/in/bala-vardhan',
        currentTitle: 'Cloud Solutions & AI Engineer',
        aboutMe: 'Software engineer specializing in AWS cloud architecture, container orchestration, Python microservices, and AI model deployment.',
        skills: ['Python', 'Java', 'AWS', 'Docker', 'Kubernetes', 'OpenAI API', 'FastAPI', 'PostgreSQL'],
        education: 'B.Tech in Computer Science - IIT Madras',
        expectedSalary: '₹14 - 22 LPA',
        workHistory: [
          {
            role: 'Cloud & AI Engineer',
            company: 'CloudMatrix Technologies',
            duration: '2022 - Present (2.5 yrs)',
            highlights: ['Architected serverless AI inference pipelines on AWS reducing latency by 40%.']
          }
        ],
        projects: [
          {
            title: 'AI Screening Pipeline',
            tech: 'Python, AWS Lambda, Docker, PostgreSQL',
            description: 'Scalable serverless document screening and text extraction engine.'
          }
        ]
      }
    ];

    // Include dynamically applied candidate applications
    const apps = this.getCandidateApplications();
    let maxId = baseApplicants.reduce((max, b) => Math.max(max, b.id || 0), 10);

    for (const app of apps) {
      if (!baseApplicants.some(b => b.job === app.jobTitle && b.email === app.candidateEmail)) {
        const isCandidateSelf = (savedProfile && (app.candidateEmail === savedProfile.email || app.candidateName.toLowerCase() === savedProfile.fullName.toLowerCase())) ||
          app.candidateEmail === 'eshwar@candidate.com' ||
          app.candidateEmail.includes('centurionuniv.edu.in');

        const applicantPhone = isCandidateSelf ? (savedProfile?.mobile || '+91 98765 43210') : '+91 98765 43210';
        const applicantLocation = isCandidateSelf ? (savedProfile?.location || app.location || 'Hyderabad, India') : (app.location || 'India');
        const applicantGithub = isCandidateSelf ? (savedProfile?.gitHub || `https://github.com/${app.candidateName.toLowerCase().replace(/\s+/g, '-')}`) : `https://github.com/${app.candidateName.toLowerCase().replace(/\s+/g, '-')}`;
        const applicantLinkedin = isCandidateSelf ? (savedProfile?.linkedIn || `https://linkedin.com/in/${app.candidateName.toLowerCase().replace(/\s+/g, '-')}`) : `https://linkedin.com/in/${app.candidateName.toLowerCase().replace(/\s+/g, '-')}`;
        const applicantTitle = isCandidateSelf ? (savedProfile?.currentTitle || app.jobTitle) : app.jobTitle;
        const applicantAbout = isCandidateSelf ? (savedProfile?.aboutMe || `Dedicated professional with proven experience in ${app.jobTitle}.`) : `Skilled candidate with strong focus on ${app.jobTitle}.`;
        const applicantSkills = isCandidateSelf && savedSkills && savedSkills.length > 0 ? savedSkills : ['Angular', 'TypeScript', 'Java', 'Spring Boot', 'SQL', 'Git', 'REST APIs'];
        const applicantEducation = isCandidateSelf ? (savedProfile?.education || 'B.Tech in Computer Science & Engineering - Centurion University') : 'B.Tech in Computer Science & Engineering';

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
          experience: '3.5 yrs',
          educationScore: 90,
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
          expectedSalary: app.salary || '₹12 - 18 LPA',
          workHistory: [
            {
              role: applicantTitle,
              company: 'Innovative Tech Solutions',
              duration: '2023 - Present (2 yrs)',
              highlights: [
                `Developed robust solutions aligned with ${app.jobTitle} technical requirements.`,
                'Contributed to cross-functional agile teams delivering scalable web systems.'
              ]
            }
          ],
          projects: [
            {
              title: `${app.jobTitle} Showcase Project`,
              tech: applicantSkills.slice(0, 4).join(', '),
              description: `End-to-end full stack application implementing best practices for ${app.jobTitle}.`
            }
          ]
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
    return [2]; // default saved job
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
