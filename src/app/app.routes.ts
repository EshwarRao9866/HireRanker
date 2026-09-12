import { Routes } from '@angular/router';

import { HomePage } from './home-page/home-page';
import { Register } from './register/register';
import { Dashboard } from './dashboard/dashboard';

import { JobPostings } from './job-postings/job-postings';
import { JobApplicants } from './job-applicants/job-applicants';
import { ResumeScreening } from './resume-screening/resume-screening';
import { EvaluationCriteria } from './evaluation-criteria/evaluation-criteria';
import { CandidateRanking } from './candidate-ranking/candidate-ranking';
import { ShortlistedCandidates } from './shortlisted-candidates/shortlisted-candidates';
import { Messages } from './messages/messages';
import { Settings } from './settings/settings';
import { Reports } from './reports/reports';
import { InterviewScheduler } from './interview-scheduler/interview-scheduler';

import { CandidateLogin } from './candidate-login/candidate-login';
import { CandidateRegister } from './candidate-register/candidate-register';
import { CandidateDashboard } from './candidate-dashboard/candidate-dashboard';
import { CandidateProfile } from './candidate-profile/candidate-profile';
import { JobList } from './job-list/job-list';
import { ResumeUpload } from './resume-upload/resume-upload';
import { MyApplications } from './my-applications/my-applications';
import { Interview } from './interview/interview';
import { InterviewRoom } from './interview-room/interview-room';

import { adminGuard } from './guards/admin.guard';
import { candidateGuard } from './guards/candidate.guard';

export const routes: Routes = [
  // ================= ADMIN PUBLIC ROUTES =================
  {
    path: '',
    component: HomePage
  },
  {
    path: 'register',
    component: Register
  },

  // ================= ADMIN PROTECTED ROUTES =================
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [adminGuard]
  },
  {
    path: 'job-postings',
    component: JobPostings,
    canActivate: [adminGuard]
  },
  {
    path: 'job-applicants',
    component: JobApplicants,
    canActivate: [adminGuard]
  },
  {
    path: 'resume-screening',
    component: ResumeScreening,
    canActivate: [adminGuard]
  },
  {
    path: 'evaluation-criteria',
    component: EvaluationCriteria,
    canActivate: [adminGuard]
  },
  {
    path: 'candidate-ranking',
    component: CandidateRanking,
    canActivate: [adminGuard]
  },
  {
    path: 'shortlisted-candidates',
    component: ShortlistedCandidates,
    canActivate: [adminGuard]
  },
  {
    path: 'messages',
    component: Messages,
    canActivate: [adminGuard]
  },
  {
    path: 'interview-scheduler',
    component: InterviewScheduler,
    canActivate: [adminGuard]
  },
  {
    path: 'reports',
    component: Reports,
    canActivate: [adminGuard]
  },
  {
    path: 'settings',
    component: Settings,
    canActivate: [adminGuard]
  },

  // ================= CANDIDATE PUBLIC ROUTES =================
  {
    path: 'candidate-login',
    component: CandidateLogin
  },
  {
    path: 'candidate-register',
    component: CandidateRegister
  },

  // ================= CANDIDATE PROTECTED ROUTES =================
  {
    path: 'candidate-dashboard',
    component: CandidateDashboard,
    canActivate: [candidateGuard]
  },
  {
    path: 'find-jobs',
    component: JobList,
    canActivate: [candidateGuard]
  },
  {
    path: 'my-resume',
    component: ResumeUpload,
    canActivate: [candidateGuard]
  },
  {
    path: 'my-applications',
    component: MyApplications,
    canActivate: [candidateGuard]
  },
  {
    path: 'interviews',
    component: Interview,
    canActivate: [candidateGuard]
  },
  {
    path: 'interview/:interviewId',
    component: InterviewRoom
  },
  {
    path: 'interview-room/:interviewId',
    redirectTo: 'interview/:interviewId'
  },
  {
    path: 'my-profile',
    component: CandidateProfile,
    canActivate: [candidateGuard]
  },

  // Legacy/Alternate route aliases redirecting to canonical candidate routes
  {
    path: 'candidate-jobs',
    redirectTo: 'find-jobs'
  },
  {
    path: 'candidate-profile',
    redirectTo: 'my-profile'
  },
  {
    path: 'resume-upload',
    redirectTo: 'my-resume'
  },
  {
    path: 'candidate-interview',
    redirectTo: 'interviews'
  },
  {
    path: 'candidate/dashboard',
    redirectTo: 'candidate-dashboard'
  },

  // ================= WILDCARD CATCH-ALL (MUST BE LAST) =================
  {
    path: '**',
    redirectTo: ''
  }
];