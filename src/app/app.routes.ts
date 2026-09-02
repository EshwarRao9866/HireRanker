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
import { CandidateDashboard } from './candidate-dashboard/candidate-dashboard';

export const routes: Routes = [

  {
    path: '',
    component: HomePage
  },

  {
    path: 'register',
    component: Register
  },

  {
    path: 'dashboard',
    component: Dashboard
  },

  {
    path: 'job-postings',
    component: JobPostings
  },

  {
    path: 'job-applicants',
    component: JobApplicants
  },

  {
    path: 'resume-screening',
    component: ResumeScreening
  },

  {
    path: 'evaluation-criteria',
    component: EvaluationCriteria
  },

  {
    path: 'candidate-ranking',
    component: CandidateRanking
  },

  {
    path: 'shortlisted-candidates',
    component: ShortlistedCandidates
  },

  {
    path: 'messages',
    component: Messages
  },

  {
    path: 'interview-scheduler',
    component: InterviewScheduler
  },

  {
    path: 'reports',
    component: Reports
  },

  {
    path: 'settings',
    component: Settings
  },

  {
    path: '**',
    redirectTo: ''
  },
  {
  path: 'candidate-dashboard',
  component: CandidateDashboard
},

];