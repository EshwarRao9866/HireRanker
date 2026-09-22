import { Injectable } from '@angular/core';
import { ScreeningResultResponse } from './screening.service';
import { CandidateProfile } from './candidate.service';
import { InterviewResponse, ScheduledInterview } from './interview.service';

@Injectable({
  providedIn: 'root'
})
export class MockDataService {

  getMockScreeningResult(applicationId: number): ScreeningResultResponse {
    return {
      id: applicationId || 0,
      applicationId: applicationId || 0,
      overallScore: 0,
      skillsScore: 0,
      experienceScore: 0,
      educationScore: 0,
      keywordScore: 0,
      projectScore: 0,
      certificationScore: 0,
      formattingScore: 0,
      achievementScore: 0,
      matchingSkills: '',
      missingSkills: '',
      recommendedSkills: '',
      strengths: '',
      weaknesses: '',
      improvementSuggestions: '',
      resumeSummary: 'No screening data available yet.',
      recommendation: 'Pending Screening',
      screenedAt: new Date().toISOString()
    };
  }

  getMockCandidateProfile(): CandidateProfile {
    return {
      id: 0,
      userId: 0,
      fullName: '',
      email: '',
      phone: '',
      location: '',
      skills: '',
      experience: '',
      education: '',
      github: '',
      linkedin: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
  }

  getMockCandidateDashboard(): any {
    return {
      totalApplications: 0,
      interviewsScheduled: 0,
      profileCompletion: 0,
      recentApplications: []
    };
  }

  getMockScheduledInterviews(): ScheduledInterview[] {
    return [];
  }

  getMockInterviewResponses(): InterviewResponse[] {
    return [];
  }
}
