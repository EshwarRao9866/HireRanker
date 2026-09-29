package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.eshwar.dto.interview.StructuredCandidateProfileDto;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.InterviewAnswer;
import spring.eshwar.entity.InterviewQuestion;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.Resume;
import spring.eshwar.service.ai.InterviewAIService;

import java.util.List;

@Service
public class AiInterviewService {

    private static final Logger log = LoggerFactory.getLogger(AiInterviewService.class);

    private final InterviewAIService interviewAiService;

    public AiInterviewService(InterviewAIService interviewAiService) {
        this.interviewAiService = interviewAiService;
    }

    public record GeneratedQuestionDto(
            String questionText,
            String category,
            String difficulty,
            String expectedConcepts,
            String keyPoints,
            String relevantSkill
    ) {
        public GeneratedQuestionDto(String questionText, String category, String difficulty) {
            this(questionText, category, difficulty, "Core architectural concepts and implementation principles", "Accuracy, practical reasoning, best practices", category);
        }
    }

    public record EvaluatedAnswerDto(
            Double technicalScore,
            Double clarityScore,
            Double completenessScore,
            Double confidenceScore,
            Double overallScore,
            String feedback,
            Boolean correct,
            String correctnessClassification,
            String explanation,
            List<String> missingConcepts,
            List<String> strengths,
            List<String> improvements
    ) {
        public EvaluatedAnswerDto(Double technicalScore, Double clarityScore, String feedback) {
            this(technicalScore, clarityScore, clarityScore, clarityScore != null ? clarityScore : 0.0,
                    technicalScore != null && clarityScore != null
                            ? Math.round(((technicalScore * 0.45) + (clarityScore * 0.25) + (clarityScore * 0.15) + (clarityScore * 0.15)) * 10.0) / 10.0
                            : 0.0,
                    feedback, (technicalScore != null && technicalScore >= 60.0),
                    (technicalScore != null && technicalScore >= 75.0 ? "CORRECT" : (technicalScore != null && technicalScore >= 50.0 ? "PARTIALLY_CORRECT" : "INCORRECT")),
                    null, java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }

        public EvaluatedAnswerDto(Double technicalScore, Double clarityScore, String feedback, Boolean correct, String explanation, List<String> missingConcepts) {
            this(technicalScore, clarityScore, clarityScore, clarityScore != null ? clarityScore : 0.0,
                    technicalScore != null && clarityScore != null
                            ? Math.round(((technicalScore * 0.45) + (clarityScore * 0.25) + (clarityScore * 0.15) + (clarityScore * 0.15)) * 10.0) / 10.0
                            : 0.0,
                    feedback, correct, (correct != null && correct ? "CORRECT" : "INCORRECT"), explanation, missingConcepts, java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }

        public EvaluatedAnswerDto(Double technicalScore, Double clarityScore, String feedback, Boolean correct,
                                  String correctnessClassification, String explanation, List<String> missingConcepts) {
            this(technicalScore, clarityScore, clarityScore, clarityScore != null ? clarityScore : 0.0,
                    technicalScore != null && clarityScore != null
                            ? Math.round(((technicalScore * 0.45) + (clarityScore * 0.25) + (clarityScore * 0.15) + (clarityScore * 0.15)) * 10.0) / 10.0
                            : 0.0,
                    feedback, correct, correctnessClassification, explanation, missingConcepts, java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }
    }

    public record GeneratedInterviewResultDto(
            Double technicalScore,
            Double communicationScore,
            Double problemSolvingScore,
            Double answerRelevanceScore,
            Double completenessScore,
            Double overallScore,
            String strengths,
            String weaknesses,
            String improvementTopics,
            String recommendation,
            String summary
    ) {}

    /**
     * Adaptively generates the next interview question based on candidate profile,
     * resume, job requirements, and past interview Q&A history.
     */
    public GeneratedQuestionDto generateNextQuestion(Candidate candidate, Resume resume, Job job,
                                                    List<InterviewQuestion> pastQuestions,
                                                    List<InterviewAnswer> pastAnswers,
                                                    int questionOrder) {
        StructuredCandidateProfileDto candidateProfile = StructuredCandidateProfileDto.fromCandidateAndResume(candidate, resume);
        return interviewAiService.generateNextQuestion(candidateProfile, job, pastQuestions, pastAnswers, questionOrder);
    }

    /**
     * Evaluates a candidate's live answer transcript with zero bias.
     */
    public EvaluatedAnswerDto evaluateAnswer(InterviewQuestion question, String answerText,
                                             Double responseTimeSeconds, Double answerDurationSeconds,
                                             Job job) {
        return interviewAiService.evaluateAnswer(question, answerText, responseTimeSeconds, answerDurationSeconds, job);
    }

    /**
     * Synthesizes the final scorecard across all questions, answers, and criteria.
     */
    public GeneratedInterviewResultDto synthesizeFinalResult(Candidate candidate, Job job,
                                                             List<InterviewQuestion> questions,
                                                             List<InterviewAnswer> answers,
                                                             double durationMinutes) {
        StructuredCandidateProfileDto candidateProfile = StructuredCandidateProfileDto.fromCandidateAndResume(candidate, null);
        return interviewAiService.synthesizeFinalResult(candidateProfile, job, questions, answers, durationMinutes);
    }
}
