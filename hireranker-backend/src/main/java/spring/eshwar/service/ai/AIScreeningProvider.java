package spring.eshwar.service.ai;

public interface AIScreeningProvider {

    /**
     * Evaluates a resume against job details and evaluation criteria.
     *
     * @param context the screening context containing job, criteria, and resume text
     * @return the structured evaluation result
     */
    ScreeningEvaluationResult evaluate(ScreeningContext context);

    /**
     * Context payload passed to the AI provider. Contains strictly necessary screening info.
     */
    record ScreeningContext(
            String jobTitle,
            String jobDescription,
            String requiredSkills,
            String experienceRequired,
            Double minimumExperience,
            String educationRequirements,
            Double skillWeight,
            Double experienceWeight,
            Double educationWeight,
            String resumeExtractedText
    ) {}

    /**
     * Structured result returned by the AI provider with Enhancv/Jobscan 8-factor ATS scoring.
     */
    record ScreeningEvaluationResult(
            Double overallScore,
            Double skillsScore,
            Double experienceScore,
            Double educationScore,
            String matchingSkills,
            String missingSkills,
            String recommendation,
            Double keywordScore,
            Double projectScore,
            Double certificationScore,
            Double formattingScore,
            Double achievementScore,
            String recommendedSkills,
            String strengths,
            String weaknesses,
            String improvementSuggestions,
            String resumeSummary
    ) {
        public ScreeningEvaluationResult(
                Double overallScore,
                Double skillsScore,
                Double experienceScore,
                Double educationScore,
                String matchingSkills,
                String missingSkills,
                String recommendation
        ) {
            this(overallScore, skillsScore, experienceScore, educationScore, matchingSkills, missingSkills, recommendation,
                    skillsScore != null ? skillsScore : 80.0,
                    experienceScore != null ? experienceScore : 75.0,
                    80.0,
                    88.0,
                    82.0,
                    "",
                    "",
                    "",
                    "",
                    "");
        }
    }
}
