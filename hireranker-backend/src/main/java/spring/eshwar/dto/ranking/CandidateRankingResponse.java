package spring.eshwar.dto.ranking;

/**
 * Backward compatibility subclass delegating to RankingResponse.
 */
public class CandidateRankingResponse extends RankingResponse {

    public CandidateRankingResponse() {
        super();
    }

    public CandidateRankingResponse(Integer rank, Long candidateId, String candidateName,
                                   Long applicationId, Double overallScore, Double skillsScore,
                                   Double experienceScore, Double educationScore, String applicationStatus) {
        super(rank, candidateId, candidateName, applicationId, overallScore, skillsScore,
                experienceScore, educationScore, applicationStatus);
    }
}
