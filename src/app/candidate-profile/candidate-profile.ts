import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CandidateService } from '../services/candidate.service';
import { ResumeService } from '../services/resume.service';

interface ProfileData {
  fullName: string;
  email: string;
  mobile: string;
  location: string;
  education: string;
  dateOfBirth: string;
  linkedIn: string;
  gitHub: string;
  currentTitle: string;
  experienceYears: number;
  preferredRole: string;
  preferredLocation: string;
  expectedSalary: string;
  aboutMe: string;
  resumeName: string;
  resumeUpdatedDate: string;
  profilePicture: string;
  completionPercentage: number;
  resumeId?: number;
}

@Component({
  selector: 'app-candidate-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './candidate-profile.html',
  styleUrl: './candidate-profile.css'
})
export class CandidateProfile implements OnInit {
  isEditing = false;
  saveMessage = '';

  // Profile Data
  profile: ProfileData = {
    fullName: 'Eshwar Rao',
    email: 'eshwar@candidate.com',
    mobile: '+91 98765 43210',
    location: 'Hyderabad, Telangana, India',
    education: 'B.Tech in Computer Science & Engineering - Centurion University (CGPA: 8.9 / 10)',
    dateOfBirth: '1998-05-14',
    linkedIn: 'https://linkedin.com/in/eshwar-rao',
    gitHub: 'https://github.com/eshwar-rao',
    currentTitle: 'Full Stack Java & Angular Developer',
    experienceYears: 4.5,
    preferredRole: 'Senior Full Stack Engineer / Tech Lead',
    preferredLocation: 'Hyderabad / Bangalore / Hybrid',
    expectedSalary: '₹14 - 18 LPA',
    aboutMe: 'Passionate Full Stack Engineer with 4.5+ years of experience designing high-performance web applications using Angular, TypeScript, Java, and Spring Boot. Strong track record in developing RESTful microservices, AI-driven recruitment platforms, and responsive user interfaces with top-tier accessibility standards.',
    resumeName: 'Eshwar_Rao_Senior_Developer_Resume.pdf',
    resumeUpdatedDate: new Date(Date.now() - 5 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
    profilePicture: '',
    completionPercentage: 85
  };

  skills: string[] = [
    'Java',
    'Spring Boot',
    'Angular',
    'TypeScript',
    'SQL',
    'Microservices'
  ];

  newSkill: string = '';

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly candidateService: CandidateService,
    private readonly resumeService: ResumeService
  ) {}

  ngOnInit(): void {
    // 1. Restore saved profile from persistent storage if present
    const savedProfile = this.authService.getCandidateProfile();
    if (savedProfile) {
      this.profile = { ...this.profile, ...savedProfile };
    }

    // 2. Restore saved skills if present
    const savedSkills = this.authService.getCandidateSkills();
    if (savedSkills && Array.isArray(savedSkills) && savedSkills.length > 0) {
      this.skills = [...savedSkills];
    }

    // 3. Keep in sync with active user session
    const session = this.authService.currentUser();
    if (session && session.fullName && !savedProfile) {
      this.profile.fullName = session.fullName;
      this.profile.email = session.email;
    }

    // 4. Fetch live profile from backend GET /api/candidates/me
    this.candidateService.getMyProfile().subscribe({
      next: (backendProfile) => {
        if (backendProfile) {
          if (backendProfile.fullName) this.profile.fullName = backendProfile.fullName;
          if (backendProfile.email) this.profile.email = backendProfile.email;
          if (backendProfile.phone) this.profile.mobile = backendProfile.phone;
          if (backendProfile.location) this.profile.location = backendProfile.location;
          if (backendProfile.experience) this.profile.aboutMe = backendProfile.experience;
          if (backendProfile.github) this.profile.gitHub = backendProfile.github;
          if (backendProfile.linkedin) this.profile.linkedIn = backendProfile.linkedin;
          if (backendProfile.skills) {
            const parsed = backendProfile.skills.split(',').map((s) => s.trim()).filter(Boolean);
            if (parsed.length > 0) this.skills = parsed;
          }
        }
      },
      error: () => {}
    });

    // 5. Synchronize with shared active resume state so uploads on /my-resume reflect immediately
    this.resumeService.activeResume$.subscribe((activeResume) => {
      if (activeResume && activeResume.fileName) {
        this.profile.resumeName = activeResume.fileName;
        if (activeResume.id) {
          this.profile.resumeId = activeResume.id;
        }
        if (activeResume.uploadedAt) {
          this.profile.resumeUpdatedDate = new Date(activeResume.uploadedAt).toLocaleDateString('en-GB', {
            day: '2-digit', month: 'short', year: 'numeric'
          });
        }
      }
    });
  }

  toggleEdit(): void {
    if (this.isEditing) {
      this.saveProfile();
    } else {
      this.isEditing = true;
      this.saveMessage = '';
    }
  }

  saveProfile(): void {
    this.isEditing = false;

    // Recalculate profile completion percentage dynamically
    let filledCount = 0;
    const requiredKeys = ['fullName', 'email', 'mobile', 'location', 'currentTitle', 'aboutMe', 'preferredRole', 'expectedSalary'];
    for (const key of requiredKeys) {
      if ((this.profile as any)[key] && String((this.profile as any)[key]).trim().length > 0) {
        filledCount++;
      }
    }
    if (this.skills.length > 0) filledCount++;
    if (this.profile.resumeName) filledCount++;
    if (this.profile.profilePicture) filledCount++;
    this.profile.completionPercentage = Math.min(100, Math.round((filledCount / (requiredKeys.length + 3)) * 100));

    // Save to persistent storage and update session
    this.authService.saveCandidateProfile(this.profile, this.skills);

    // Save to backend PUT /api/candidates/me
    this.candidateService.updateMyProfile({
      fullName: this.profile.fullName,
      phone: this.profile.mobile,
      location: this.profile.location,
      skills: this.skills.join(', '),
      experience: this.profile.aboutMe,
      github: this.profile.gitHub,
      linkedin: this.profile.linkedIn
    }).subscribe({
      next: () => {},
      error: (err) => console.warn('Could not sync profile to backend:', err?.status)
    });

    this.saveMessage = 'Profile updated and saved successfully!';
    setTimeout(() => {
      this.saveMessage = '';
    }, 4000);
  }

  onProfilePictureSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        alert('Please select a valid image file (PNG, JPG, WEBP, etc.)');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        alert('Image size exceeds 5MB limit. Please select a smaller photo.');
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        this.profile.profilePicture = reader.result as string;
        this.authService.saveCandidateProfile(this.profile, this.skills);
        this.saveMessage = 'Profile picture updated successfully!';
        setTimeout(() => (this.saveMessage = ''), 3000);
      };
      reader.readAsDataURL(file);
    }
  }

  removeProfilePicture(): void {
    this.profile.profilePicture = '';
    this.authService.saveCandidateProfile(this.profile, this.skills);
    this.saveMessage = 'Profile picture removed.';
    setTimeout(() => (this.saveMessage = ''), 3000);
  }

  addSkill(): void {
    if (this.newSkill.trim() && !this.skills.includes(this.newSkill.trim())) {
      this.skills.push(this.newSkill.trim());
      this.newSkill = '';
      this.authService.saveCandidateProfile(this.profile, this.skills);
    }
  }

  removeSkill(skill: string): void {
    this.skills = this.skills.filter(s => s !== skill);
    this.authService.saveCandidateProfile(this.profile, this.skills);
  }

  uploadResume(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      if (!file.name.toLowerCase().endsWith('.pdf') && !file.name.toLowerCase().endsWith('.doc') && !file.name.toLowerCase().endsWith('.docx')) {
        this.saveMessage = 'Please select a valid PDF or Word document.';
        setTimeout(() => (this.saveMessage = ''), 3500);
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        this.saveMessage = 'File size exceeds 10MB limit.';
        setTimeout(() => (this.saveMessage = ''), 3500);
        return;
      }
      if (file.size === 0) {
        this.saveMessage = 'The selected file is empty. Please select a valid document.';
        setTimeout(() => (this.saveMessage = ''), 3500);
        return;
      }

      this.profile.resumeName = file.name;
      this.profile.resumeUpdatedDate = new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
      this.authService.saveCandidateProfile(this.profile, this.skills);

      this.resumeService.uploadResume(file).subscribe({
        next: (res) => {
          this.profile.resumeId = res.id;
          this.profile.resumeName = res.fileName;
          this.resumeService.setActiveResume(res);
          this.authService.saveCandidateProfile(this.profile, this.skills);
          this.saveMessage = `Resume "${res.fileName}" uploaded and parsed by AI!`;
          setTimeout(() => (this.saveMessage = ''), 3500);
        },
        error: () => {
          this.saveMessage = `Resume "${file.name}" saved to profile.`;
          setTimeout(() => (this.saveMessage = ''), 3500);
        }
      });
    }
  }

  viewResume(): void {
    if (!this.profile.resumeName || this.profile.resumeName === 'No resume uploaded yet') {
      this.saveMessage = 'Please upload a resume first.';
      setTimeout(() => (this.saveMessage = ''), 3500);
      return;
    }

    const obs$ = this.profile.resumeId
      ? this.resumeService.downloadResumeBlob(this.profile.resumeId)
      : this.resumeService.getMyResumeBlob();

    obs$.subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.saveMessage = 'Resume file is currently unavailable.';
          setTimeout(() => (this.saveMessage = ''), 3500);
          return;
        }
        const blobUrl = window.URL.createObjectURL(blob);
        window.open(blobUrl, '_blank');
      },
      error: () => {
        this.saveMessage = 'Unable to view resume PDF. Please check server connection or re-upload your resume.';
        setTimeout(() => (this.saveMessage = ''), 4000);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/candidate-dashboard']);
  }

  openJobs(): void {
    this.router.navigate(['/find-jobs']);
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

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}
