package spring.eshwar.dto.dashboard;

import spring.eshwar.dto.candidate.CandidateRecentApplicationDTO;
import spring.eshwar.dto.candidate.CandidateResponse;
import spring.eshwar.dto.candidate.RecommendedJobDTO;
import spring.eshwar.dto.interview.InterviewResponse;

import java.util.List;
import java.util.Map;

/**
 * DTO representing candidate dashboard overview metrics.
 */
public class CandidateDashboardResponse extends spring.eshwar.dto.candidate.CandidateDashboardResponse {

    public CandidateDashboardResponse() {
        super();
    }

    public CandidateDashboardResponse(CandidateResponse candidateProfile,
                                    long totalApplications,
                                    long totalResumes,
                                    long screenedApplications,
                                    long shortlistedApplications,
                                    List<InterviewResponse> upcomingInterviews,
                                    List<CandidateRecentApplicationDTO> recentApplications,
                                    Map<String, Long> applicationStatuses,
                                    List<RecommendedJobDTO> recommendedJobs) {
        super(candidateProfile, totalApplications, totalResumes, screenedApplications, shortlistedApplications,
                upcomingInterviews, recentApplications, applicationStatuses, recommendedJobs);
    }
}
