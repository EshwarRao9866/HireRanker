package spring.eshwar.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spring.eshwar.dto.interview.StructuredCandidateProfileDto;
import spring.eshwar.entity.InterviewAnswer;
import spring.eshwar.entity.InterviewQuestion;
import spring.eshwar.entity.Job;
import spring.eshwar.service.AiInterviewService.EvaluatedAnswerDto;
import spring.eshwar.service.AiInterviewService.GeneratedInterviewResultDto;
import spring.eshwar.service.AiInterviewService.GeneratedQuestionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task-specific AI service for live voice interviews.
 * Utilizes OpenRouter / Groq with intelligent prompt engineering,
 * dynamic difficulty calibration, multi-category non-repetitive progression,
 * semantic answer evaluation with constructive explanations, and multi-dimensional scoring.
 */
@Service
public class InterviewAIService {

    private static final Logger log = LoggerFactory.getLogger(InterviewAIService.class);

    private static final String[] QUESTION_CATEGORIES = {
            "Language Fundamentals & Core Mechanisms",
            "Framework Architecture & Lifecycle",
            "API, Integration & Data Flow",
            "Database, Query Optimization & Transactions",
            "Frontend/Client Architecture & Reactivity",
            "Practical Application & Edge-Case Debugging",
            "Security, Authentication & Authorization",
            "Concurrency, Threading & Performance",
            "Microservices, Resilience & System Design",
            "Production Scenarios, Observability & Outages"
    };

    private final ObjectMapper objectMapper;
    private final OpenRouterApiClient openRouterApiClient;
    private final GroqApiClient groqApiClient;

    @Value("${application.ai.interview.model:${application.ai.openrouter.interview-model:${INTERVIEW_AI_MODEL:openai/gpt-oss-20b}}}")
    private String interviewModel;

    @Value("${application.ai.interview.evaluation-model:${INTERVIEW_EVAL_AI_MODEL:openai/gpt-oss-120b}}")
    private String evaluationModel;

    @Value("${application.ai.openrouter.fallback-model:${AI_FALLBACK_MODEL:openrouter/free}}")
    private String fallbackModel;

    public InterviewAIService(ObjectMapper objectMapper,
                              OpenRouterApiClient openRouterApiClient,
                              GroqApiClient groqApiClient) {
        this.objectMapper = objectMapper;
        this.openRouterApiClient = openRouterApiClient;
        this.groqApiClient = groqApiClient;
    }

    public String getInterviewModel() {
        return interviewModel;
    }

    public String getEvaluationModel() {
        return evaluationModel;
    }

    /**
     * Adaptively generates the next interview question across dynamic progression
     * with strict non-repetition constraints, multi-skill coverage, and adaptive difficulty calibration.
     */
    public GeneratedQuestionDto generateNextQuestion(StructuredCandidateProfileDto candidate,
                                                     Job job,
                                                     List<InterviewQuestion> pastQuestions,
                                                     List<InterviewAnswer> pastAnswers,
                                                     int questionOrder) {
        String candidateName = candidate != null ? candidate.candidateName() : "Candidate";
        List<String> prioritizedSkills = candidate != null ? candidate.getPrioritizedSkillList(job) : List.of("Java", "Spring Boot", "SQL", "REST", "System Architecture");
        String candidateSkills = candidate != null ? String.join(", ", candidate.coreSkills()) : "Java, Spring Boot, SQL, Docker";
        String candidateExp = candidate != null ? candidate.yearsOfExperience() : "3+ years";
        String candidateProjects = candidate != null ? candidate.projectsSummary() : "Enterprise Web Services";

        String jobTitle = job != null && job.getTitle() != null ? job.getTitle() : "Software Engineer";
        String jobSkills = job != null && job.getRequiredSkills() != null ? job.getRequiredSkills() : "Problem Solving, System Design";
        String jobDesc = job != null && job.getDescription() != null ? job.getDescription() : "";

        int categoryIndex = Math.max(0, Math.min(QUESTION_CATEGORIES.length - 1, questionOrder - 1));
        String targetCategory = QUESTION_CATEGORIES[categoryIndex];

        // Identify unasked skills first from the prioritized list to guarantee broad skill coverage
        List<String> alreadyAskedSkills = new ArrayList<>();
        if (pastQuestions != null) {
            for (InterviewQuestion pq : pastQuestions) {
                if (pq.getCategory() != null) alreadyAskedSkills.add(pq.getCategory().toLowerCase());
                // Also check if any prioritized skill name is present in question or stored concepts
                for (String s : prioritizedSkills) {
                    if (pq.getQuestionText() != null && pq.getQuestionText().toLowerCase().contains(s.toLowerCase())) {
                        if (!alreadyAskedSkills.contains(s.toLowerCase())) {
                            alreadyAskedSkills.add(s.toLowerCase());
                        }
                    }
                }
            }
        }

        // Pick next unasked prioritized skill; if all covered, rotate cleanly through prioritized skills
        String targetSkill = null;
        for (String s : prioritizedSkills) {
            if (!alreadyAskedSkills.contains(s.toLowerCase())) {
                targetSkill = s;
                break;
            }
        }
        if (targetSkill == null || targetSkill.isBlank()) {
            targetSkill = prioritizedSkills.get((questionOrder - 1) % prioritizedSkills.size());
        }

        // Dynamic difficulty calibration based on past performance
        String targetDifficulty = "Intermediate";
        String performanceTrend = "Neutral / First Question";
        if (pastAnswers != null && !pastAnswers.isEmpty()) {
            InterviewAnswer lastAnswer = pastAnswers.get(pastAnswers.size() - 1);
            Double lastScore = lastAnswer != null ? lastAnswer.getTechnicalScore() : null;

            double totalScore = 0.0;
            int scoredCount = 0;
            for (InterviewAnswer a : pastAnswers) {
                if (a.getTechnicalScore() != null) {
                    totalScore += a.getTechnicalScore();
                    scoredCount++;
                }
            }
            if (scoredCount > 0) {
                double avg = totalScore / scoredCount;
                if (lastScore != null && lastScore >= 80.0) {
                    targetDifficulty = (avg >= 70.0) ? "Advanced" : "Intermediate";
                    performanceTrend = String.format("Strong preceding answer (Score: %.1f): Elevate architectural depth or pivot to next priority skill '%s'", lastScore, targetSkill);
                } else if (lastScore != null && lastScore < 50.0) {
                    targetDifficulty = "Easy";
                    performanceTrend = String.format("Weak preceding answer (Score: %.1f): Probe foundational concepts and practical fundamentals without repeating prior prompt", lastScore);
                } else {
                    targetDifficulty = "Intermediate";
                    performanceTrend = String.format("Consistent progression (Average: %.1f): Maintain balanced practical inquiry on '%s'", avg, targetSkill);
                }
            }
        }

        StringBuilder historyBuilder = new StringBuilder();
        StringBuilder askedTopicsBuilder = new StringBuilder();
        if (pastQuestions != null && !pastQuestions.isEmpty()) {
            for (int i = 0; i < pastQuestions.size(); i++) {
                InterviewQuestion q = pastQuestions.get(i);
                historyBuilder.append("Q").append(q.getQuestionOrder()).append(" [")
                        .append(q.getCategory()).append("]: ").append(q.getQuestionText()).append("\n");
                askedTopicsBuilder.append("- Q").append(q.getQuestionOrder()).append(" [")
                        .append(q.getCategory()).append("]: \"").append(q.getQuestionText()).append("\"\n");
                if (pastAnswers != null && i < pastAnswers.size()) {
                    InterviewAnswer a = pastAnswers.get(i);
                    String ansText = (a != null && a.getAnswerText() != null) ? a.getAnswerText() : "[Skipped / 15s Timeout]";
                    Double score = (a != null && a.getTechnicalScore() != null) ? a.getTechnicalScore() : 0.0;
                    historyBuilder.append("A").append(q.getQuestionOrder()).append(" (Score: ").append(score).append("): ").append(ansText).append("\n");
                }
            }
        }

        String prompt = String.format("""
                You are an expert, unbiased AI Technical Interviewer conducting a live adaptive audio interview for HireRanker.
                Candidate: %s
                Title/Experience: %s (%s)
                Candidate Resume Skills: %s
                Key Projects: %s

                Target Job: %s
                Job Required Skills: %s
                Job Description Excerpt: %s

                Prioritized Skill Progression: %s
                Current Target Skill: %s
                Interview Progression: Question %d (Adaptive Session: 6 to 10 Questions Total)
                TARGET CATEGORY: %s
                CALIBRATED DIFFICULTY: %s
                PERFORMANCE TREND: %s

                PAST QUESTIONS & ANSWERS IN THIS SESSION:
                %s

                CRITICAL ADAPTIVE INTERVIEW RULES:
                1. STRICT NON-REPETITION & SKILL DIVERSITY:
                   - You MUST NOT repeat any question wording, exact scenario, or underlying concept already asked.
                   - NEVER ask multiple variations of the same tool or language (e.g., do not ask ten Java questions).
                   - Cover diverse areas across:
                     * Core programming / language fundamentals
                     * Framework architecture & lifecycle
                     * API design & data flow
                     * Database indexing, transactions & query optimization
                     * Client/frontend architecture (or systems integration if backend-focused)
                     * Practical debugging & edge cases
                     * Concurrency, security, resilience, or microservices
                2. RELEVANCE TO JOB & RESUME:
                   - Target Skill for this question is '%s'. Formulate the question around this skill within category '%s'.
                   - Prioritize skills required by the job first, then candidate's verified resume background.
                3. ADAPTIVE DEPTH & PROGRESSION:
                   - If the candidate answered strongly, ask deeper architectural or trade-off questions.
                   - If the candidate struggled, ask a simpler, foundational practical question without echoing prior words.
                   - Do not repeatedly ask the same concept if the candidate previously answered it.
                4. CONCISE VERBAL PHRASING:
                   - Formulate the question in 1 to 2 clear sentences maximum.
                   - Must sound natural and engaging when spoken aloud via Text-to-Speech.
                5. Output strictly valid JSON matching this schema:
                   {
                     "question": "<concise verbal question in 1-2 sentences>",
                     "topic": "%s",
                     "difficulty": "%s",
                     "expectedConcepts": "<comma-separated essential technical mechanisms, concepts, and algorithms>",
                     "keyPoints": "<concise bullet-style criteria for what a high-scoring answer must include>",
                     "relevantSkill": "%s",
                     "reason": "<brief rationale for this question>"
                   }
                """,
                candidateName, candidate != null ? candidate.currentTitle() : "Engineer", candidateExp,
                candidateSkills, candidateProjects, jobTitle, jobSkills, jobDesc,
                String.join(" -> ", prioritizedSkills), targetSkill, questionOrder,
                targetCategory, targetDifficulty, performanceTrend,
                askedTopicsBuilder.isEmpty() ? "No prior questions asked yet. This is Question 1." : askedTopicsBuilder.toString(),
                targetSkill, targetCategory,
                targetCategory, targetDifficulty, targetSkill
        );

        // 1. Primary: OpenRouter model
        if (openRouterApiClient.isConfigured()) {
            try {
                String openRouterJson = openRouterApiClient.callChatCompletion(
                        interviewModel,
                        "You are an AI Technical Interviewer. Output strictly valid JSON with keys: question, topic, difficulty, expectedConcepts, keyPoints, relevantSkill, reason.",
                        prompt,
                        true,
                        0.2,
                        1200
                );
                GeneratedQuestionDto parsed = parseQuestionJson(openRouterJson, targetCategory, targetDifficulty, targetSkill);
                if (parsed != null) return parsed;
            } catch (Exception ex) {
                log.warn("OpenRouter interview question generation failed: {}. Triggering Groq fallback.", ex.getMessage());
            }
        }

        // 2. Secondary Fallback: Groq (LPU low-latency inference)
        if (groqApiClient.isConfigured()) {
            try {
                String groqJson = groqApiClient.callChatCompletion(
                        "You are an AI Technical Interviewer. Output strictly valid JSON with keys: question, topic, difficulty, expectedConcepts, keyPoints, relevantSkill, reason.",
                        prompt,
                        true
                );
                GeneratedQuestionDto parsed = parseQuestionJson(groqJson, targetCategory, targetDifficulty, targetSkill);
                if (parsed != null) return parsed;
            } catch (Exception ex) {
                log.warn("Groq secondary fallback failed: {}", ex.getMessage());
            }
        }

        // 3. Deterministic diverse fallback
        return fallbackQuestion(candidateName, prioritizedSkills, jobTitle, jobSkills, questionOrder, pastQuestions);
    }

    /**
     * Evaluates a candidate's live answer transcript with zero bias and semantic understanding.
     * Returns whether the answer is technically correct, technical/clarity scores, and a constructive explanation.
     */
    public EvaluatedAnswerDto evaluateAnswer(InterviewQuestion question,
                                            String answerText,
                                            Double responseTimeSeconds,
                                            Double answerDurationSeconds,
                                            Job job) {
        if (answerText == null || answerText.trim().isEmpty()) {
            return new EvaluatedAnswerDto(
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    "No spoken answer detected (15-second speech countdown expired).",
                    false,
                    "INCORRECT",
                    "No answer was provided before the response window closed.",
                    List.of("Conceptual understanding"),
                    List.of(),
                    List.of("Attempt to answer before the 15-second countdown expires.")
            );
        }

        String expectedConcepts = (question.getExpectedConcepts() != null && !question.getExpectedConcepts().isBlank())
                ? question.getExpectedConcepts()
                : "Core mechanisms, execution flow, architectural trade-offs, and failure modes";
        String keyPoints = (question.getKeyPoints() != null && !question.getKeyPoints().isBlank())
                ? question.getKeyPoints()
                : "Technical accuracy, concrete practical reasoning, best practices";

        String prompt = String.format("""
                You are a strict, objective, and unbiased AI Technical Interview Evaluator.
                Evaluate the candidate's spoken response strictly against the question and expected concepts.

                QUESTION CONTEXT:
                - Target Role: %s
                - Category: %s
                - Difficulty: %s
                - Question: %s
                - Expected Concepts: %s
                - Key Evaluation Points: %s

                CANDIDATE SPOKEN RESPONSE:
                - Transcript: "%s"
                - Response Latency: %.1f seconds | Spoken Duration: %.1f seconds

                SCORING PRINCIPLES & GUIDELINES:
                1. NO FAKE SCORES:
                   - Scores must be strictly earned from the actual candidate transcript content.
                   - DO NOT award default scores (e.g., 80, 85, 90) simply because words were spoken.
                   - If the answer is completely evasive, gibberish, or irrelevant, technicalScore must be <= 20.
                   - If the candidate demonstrates accurate mastery of the expected concepts, award a high score (80-100).
                   - If partially correct with missing essential mechanisms, award a proportional partial score (40-75).
                2. EVALUATION DIMENSIONS (All 0 to 100):
                   - technicalScore (0-100): Correctness, accuracy of technical explanations, and avoidance of major mistakes.
                   - clarityScore (0-100): Clear structure, coherent phrasing, and precision of technical vocabulary.
                   - completenessScore (0-100): Degree to which all required expected concepts and key points were addressed.
                   - confidenceScore (0-100): Observable communication fluency strictly derived from speech characteristics:
                     * Higher score: Fluent, direct answers, concise sentences, minimal hesitation.
                     * Lower score: Excessive verbal fillers ("um", "like", "you know"), heavy repetition, trailing unfinished sentences.
                     * NEVER judge confidence based on perceived personality, psychological traits, or any protected characteristics.
                   - overallScore (0-100): Strictly calculated weighted formula:
                     overallScore = (technicalScore * 0.45) + (completenessScore * 0.25) + (clarityScore * 0.15) + (confidenceScore * 0.15)
                3. CLASSIFICATION:
                   - "CORRECT" (technicalScore >= 75 and essential concepts addressed)
                   - "PARTIALLY_CORRECT" (technicalScore between 45 and 74)
                   - "INCORRECT" (technicalScore < 45 or fundamentally flawed)
                4. CONSTRUCTIVE EXPLANATION:
                   - If incorrect or partially correct, explain what the correct concept was and list missingConcepts.
                   - If correct, provide positive affirmation and technical nuances.

                Output strictly valid JSON with this exact schema:
                {
                  "correct": <true|false>,
                  "correctnessClassification": "<CORRECT|PARTIALLY_CORRECT|INCORRECT>",
                  "technicalScore": <0.0-100.0>,
                  "clarityScore": <0.0-100.0>,
                  "completenessScore": <0.0-100.0>,
                  "confidenceScore": <0.0-100.0>,
                  "overallScore": <0.0-100.0>,
                  "feedback": "<concise constructive feedback in 1-2 sentences>",
                  "explanation": "<concise 1-2 sentence spoken explanation of the concept>",
                  "missingConcepts": ["<missing concept 1>", "<missing concept 2>"],
                  "strengths": ["<candidate strength demonstrated>"],
                  "improvements": ["<concrete topic to review>"]
                }
                """,
                job != null ? job.getTitle() : "Software Engineer",
                question.getCategory(),
                question.getDifficulty(),
                question.getQuestionText(),
                expectedConcepts,
                keyPoints,
                answerText.trim(),
                responseTimeSeconds != null ? responseTimeSeconds : 0.0,
                answerDurationSeconds != null ? answerDurationSeconds : 0.0
        );

        // 1. Primary: OpenRouter model (using stronger reasoning-capable model)
        if (openRouterApiClient.isConfigured()) {
            try {
                String openRouterJson = openRouterApiClient.callChatCompletion(
                        evaluationModel,
                        "You are an objective technical interview evaluator. Output strictly JSON.",
                        prompt,
                        true,
                        0.1,
                        1200
                );
                EvaluatedAnswerDto parsed = parseAnswerEvaluationJson(openRouterJson);
                if (parsed != null) return parsed;

                // Single structured retry if first response had malformed JSON
                log.info("Retrying structured evaluation via OpenRouter '{}'...", evaluationModel);
                String retryJson = openRouterApiClient.callChatCompletion(
                        evaluationModel,
                        "Your previous response had malformed JSON. Return strictly valid JSON matching the schema.",
                        prompt,
                        true,
                        0.1,
                        1200
                );
                parsed = parseAnswerEvaluationJson(retryJson);
                if (parsed != null) return parsed;
            } catch (Exception ex) {
                log.warn("OpenRouter interview answer evaluation with '{}' failed: {}. Triggering Groq fallback.", evaluationModel, ex.getMessage());
            }
        }

        // 2. Secondary Fallback: Groq
        if (groqApiClient.isConfigured()) {
            try {
                String groqJson = groqApiClient.callChatCompletion("You are an objective technical interview evaluator. Output strictly JSON.", prompt, true);
                EvaluatedAnswerDto parsed = parseAnswerEvaluationJson(groqJson);
                if (parsed != null) return parsed;

                // Single structured retry
                log.info("Retrying structured evaluation via Groq fallback...");
                String retryJson = groqApiClient.callChatCompletion("Return strictly valid JSON matching the schema.", prompt, true);
                parsed = parseAnswerEvaluationJson(retryJson);
                if (parsed != null) return parsed;
            } catch (Exception ex) {
                log.warn("Groq secondary answer evaluation failed: {}", ex.getMessage());
            }
        }

        // 3. Deterministic objective evaluation fallback (No fake scores)
        return fallbackAnswerEvaluation(answerText, question);
    }

    /**
     * Synthesizes the final interview report scorecard using the multi-dimensional formula:
     * - Technical Score: 50%
     * - Communication Score: 20%
     * - Clarity Score: 10%
     * - Relevance Score: 10%
     * - Problem Solving Score: 10%
     */
    public GeneratedInterviewResultDto synthesizeFinalResult(StructuredCandidateProfileDto candidate,
                                                            Job job,
                                                            List<InterviewQuestion> questions,
                                                            List<InterviewAnswer> answers,
                                                            double durationMinutes) {
        String candidateName = candidate != null ? candidate.candidateName() : "Candidate";
        String jobTitle = job != null && job.getTitle() != null ? job.getTitle() : "Software Engineer";

        StringBuilder transcript = new StringBuilder();
        double sumTech = 0.0;
        double sumClarity = 0.0;
        int evaluatedCount = 0;

        for (int i = 0; i < questions.size(); i++) {
            InterviewQuestion q = questions.get(i);
            transcript.append(String.format("Q%d [%s]: %s\n", q.getQuestionOrder(), q.getCategory(), q.getQuestionText()));
            if (i < answers.size()) {
                InterviewAnswer a = answers.get(i);
                transcript.append(String.format("A%d: %s\n", q.getQuestionOrder(), a.getAnswerText() != null ? a.getAnswerText() : "[Skipped]"));
                if (a.getTechnicalScore() != null) {
                    sumTech += a.getTechnicalScore();
                    sumClarity += (a.getClarityScore() != null ? a.getClarityScore() : a.getTechnicalScore());
                    evaluatedCount++;
                }
            }
        }

        String prompt = String.format("""
                Synthesize the final interview scorecard for HireRanker with zero bias:
                Candidate: %s
                Role: %s
                Duration: %.1f minutes

                Full Q&A Transcript:
                %s

                SCORING DIMENSIONS:
                - technicalScore (0-100): Core subject matter depth and correctness
                - communicationScore (0-100): Verbal articulation, coherence, and conciseness
                - problemSolvingScore (0-100): Structured analytical thinking and edge-case handling
                - answerRelevanceScore (0-100): Direct alignment to question intent without deflection
                - completenessScore (0-100): Breadth of coverage and thoroughness

                Output strictly valid JSON:
                {
                  "technicalScore": <0.0 - 100.0>,
                  "communicationScore": <0.0 - 100.0>,
                  "problemSolvingScore": <0.0 - 100.0>,
                  "answerRelevanceScore": <0.0 - 100.0>,
                  "completenessScore": <0.0 - 100.0>,
                  "strengths": "- Key observed technical strengths across responses",
                  "weaknesses": "- Specific technical growth areas or missing depth",
                  "improvementTopics": "Specific tools or design patterns to study",
                  "recommendation": "STRONG_HIRE | HIRE | CONSIDER | DO_NOT_HIRE",
                  "summary": "Concise executive performance summary"
                }
                """, candidateName, jobTitle, durationMinutes, transcript);

        // 1. Primary: OpenRouter model
        if (openRouterApiClient.isConfigured()) {
            try {
                String openRouterJson = openRouterApiClient.callChatCompletion(
                        interviewModel,
                        "You are an executive hiring panel. Output strictly JSON.",
                        prompt,
                        true,
                        0.1,
                        1500
                );
                GeneratedInterviewResultDto parsed = parseResultJson(openRouterJson, jobTitle);
                if (parsed != null) return parsed;
            } catch (Exception ex) {
                log.warn("OpenRouter interview result synthesis failed: {}. Triggering Groq fallback.", ex.getMessage());
            }
        }

        // 2. Secondary Fallback: Groq
        if (groqApiClient.isConfigured()) {
            try {
                String groqJson = groqApiClient.callChatCompletion("You are an executive hiring panel. Output strictly JSON.", prompt, true);
                GeneratedInterviewResultDto parsed = parseResultJson(groqJson, jobTitle);
                if (parsed != null) return parsed;
            } catch (Exception ex) {
                log.warn("Groq secondary result synthesis failed: {}", ex.getMessage());
            }
        }

        // 3. Fallback deterministic multi-dimensional calculation
        double avgTech = evaluatedCount > 0 ? (sumTech / evaluatedCount) : 75.0;
        double avgClarity = evaluatedCount > 0 ? (sumClarity / evaluatedCount) : 78.0;
        double commScore = Math.min(100.0, avgClarity * 1.02);
        double probScore = Math.min(100.0, avgTech * 0.96);
        double relScore = Math.min(100.0, avgTech * 0.98);
        double clarityScore = avgClarity;

        // Weighted Overall Score: Tech 50%, Comm 20%, Clarity 10%, Relevance 10%, Problem Solving 10%
        double overall = Math.round(
                ((avgTech * 0.50) + (commScore * 0.20) + (clarityScore * 0.10) + (relScore * 0.10) + (probScore * 0.10)) * 10.0
        ) / 10.0;

        String rec = overall >= 85.0 ? "STRONG_HIRE" : (overall >= 70.0 ? "HIRE" : (overall >= 55.0 ? "CONSIDER" : "DO_NOT_HIRE"));

        return new GeneratedInterviewResultDto(
                avgTech,
                commScore,
                probScore,
                relScore,
                clarityScore,
                overall,
                "- Clear technical fundamentals demonstrated across core topics\n- Solid communication of software engineering principles",
                "- Could elaborate further on production scale, distributed systems, and edge cases",
                "Distributed systems, automated deployment pipelines, resilience patterns",
                rec,
                String.format("Candidate completed live technical assessment for %s with an overall score of %.1f%%.", jobTitle, overall)
        );
    }

    private GeneratedQuestionDto parseQuestionJson(String jsonStr, String defaultCategory, String defaultDifficulty, String defaultSkill) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(cleanJson(jsonStr));
            String q = root.path("question").asText(root.path("questionText").asText(""));
            String cat = root.path("topic").asText(root.path("category").asText(defaultCategory));
            String diff = root.path("difficulty").asText(defaultDifficulty);
            String concepts = root.path("expectedConcepts").asText(root.path("expected_concepts").asText("Core mechanisms, execution flow, architectural principles"));
            String points = root.path("keyPoints").asText(root.path("key_points").asText("Technical accuracy, practical reasoning, best practices"));
            String skill = root.path("relevantSkill").asText(root.path("skill").asText(defaultSkill));

            if (!q.isBlank()) {
                return new GeneratedQuestionDto(q.trim(), cat, diff, concepts.trim(), points.trim(), skill.trim());
            }
        } catch (Exception e) {
            log.warn("Failed to parse question JSON: {}.", e.getMessage());
        }
        return null;
    }

    private EvaluatedAnswerDto parseAnswerEvaluationJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(cleanJson(jsonStr));

            // Must contain at least technicalScore or correctnessClassification
            if (!root.has("technicalScore") && !root.has("correctnessClassification") && !root.has("correct")) {
                log.warn("Evaluation JSON missing critical fields: {}", jsonStr);
                return null;
            }

            double tech = Math.max(0.0, Math.min(100.0, root.path("technicalScore").asDouble(0.0)));
            double clarity = Math.max(0.0, Math.min(100.0, root.path("clarityScore").asDouble(tech)));
            double completeness = Math.max(0.0, Math.min(100.0, root.path("completenessScore").asDouble(tech)));
            double confidence = Math.max(0.0, Math.min(100.0, root.path("confidenceScore").asDouble(clarity)));

            // Enforce consistent weighted overall score calculation:
            // overallScore = (technicalScore * 0.45) + (completenessScore * 0.25) + (clarityScore * 0.15) + (confidenceScore * 0.15)
            // Clamped strictly between 0.0 and 100.0, rounded to 1 decimal place.
            double overall = Math.round(
                    ((tech * 0.45) + (completeness * 0.25) + (clarity * 0.15) + (confidence * 0.15)) * 10.0
            ) / 10.0;
            overall = Math.max(0.0, Math.min(100.0, overall));

            boolean correct = root.has("correct") ? root.path("correct").asBoolean() : (tech >= 60.0);
            String classification = root.path("correctnessClassification").asText(null);
            if (classification == null || classification.isBlank()) {
                if (tech >= 75.0) {
                    classification = "CORRECT";
                } else if (tech >= 45.0) {
                    classification = "PARTIALLY_CORRECT";
                } else {
                    classification = "INCORRECT";
                }
            } else {
                classification = classification.trim().toUpperCase();
                if (!List.of("CORRECT", "PARTIALLY_CORRECT", "INCORRECT").contains(classification)) {
                    classification = (tech >= 75.0) ? "CORRECT" : (tech >= 45.0 ? "PARTIALLY_CORRECT" : "INCORRECT");
                }
            }

            String explanation = root.path("explanation").asText(
                    correct ? "Correct technical concept." : "A complete answer should address core mechanisms and system behavior."
            );

            List<String> missingConcepts = new ArrayList<>();
            if (root.has("missingConcepts") && root.path("missingConcepts").isArray()) {
                for (JsonNode item : root.path("missingConcepts")) {
                    String val = item.asText("").trim();
                    if (!val.isEmpty()) missingConcepts.add(val);
                }
            }

            List<String> strengths = new ArrayList<>();
            if (root.has("strengths") && root.path("strengths").isArray()) {
                for (JsonNode item : root.path("strengths")) {
                    String val = item.asText("").trim();
                    if (!val.isEmpty()) strengths.add(val);
                }
            }

            List<String> improvements = new ArrayList<>();
            if (root.has("improvements") && root.path("improvements").isArray()) {
                for (JsonNode item : root.path("improvements")) {
                    String val = item.asText("").trim();
                    if (!val.isEmpty()) improvements.add(val);
                }
            }

            String fb = root.path("feedback").asText(correct ? "Accurate technical response covering core concepts." : "Response missed key mechanisms required for this question.");

            return new EvaluatedAnswerDto(
                    tech,
                    clarity,
                    completeness,
                    confidence,
                    overall,
                    fb,
                    correct,
                    classification,
                    explanation,
                    missingConcepts,
                    strengths,
                    improvements
            );
        } catch (Exception e) {
            log.warn("Failed to parse answer evaluation JSON: {}.", e.getMessage());
        }
        return null;
    }

    private GeneratedInterviewResultDto parseResultJson(String jsonStr, String jobTitle) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(cleanJson(jsonStr));
            double tech = root.path("technicalScore").asDouble(75.0);
            double comm = root.path("communicationScore").asDouble(80.0);
            double prob = root.path("problemSolvingScore").asDouble(75.0);
            double rel = root.path("answerRelevanceScore").asDouble(78.0);
            double clarity = root.path("completenessScore").asDouble(75.0);

            // Calculate overall score strictly using 50/20/10/10/10 weighted formula
            double overall = Math.round(
                    ((tech * 0.50) + (comm * 0.20) + (clarity * 0.10) + (rel * 0.10) + (prob * 0.10)) * 10.0
            ) / 10.0;

            String strengths = root.path("strengths").asText("Solid understanding of software design and development standards.");
            String weaknesses = root.path("weaknesses").asText("Can deepen discussion on scaling, caching, and resiliency.");
            String improvementTopics = root.path("improvementTopics").asText("Distributed systems, observability, CI/CD automation");
            String rec = root.path("recommendation").asText(
                    overall >= 85.0 ? "STRONG_HIRE" : (overall >= 70.0 ? "HIRE" : (overall >= 55.0 ? "CONSIDER" : "DO_NOT_HIRE"))
            );
            String summary = root.path("summary").asText(String.format("Candidate demonstrated competent technical knowledge for %s.", jobTitle));

            return new GeneratedInterviewResultDto(
                    tech, comm, prob, rel, clarity, overall, strengths, weaknesses, improvementTopics, rec, summary
            );
        } catch (Exception e) {
            log.warn("Failed to parse final result JSON: {}.", e.getMessage());
        }
        return null;
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.trim();
    }

    private GeneratedQuestionDto fallbackQuestion(String candidateName, List<String> prioritizedSkills,
                                                  String jobTitle, String jobSkills, int order,
                                                  List<InterviewQuestion> pastQuestions) {
        List<String> skillsList = (prioritizedSkills != null && !prioritizedSkills.isEmpty())
                ? new ArrayList<>(prioritizedSkills)
                : List.of("Java", "Spring Boot", "SQL", "REST APIs", "System Architecture", "Concurrency", "Microservices");

        // Pick distinct skill for each question order to prevent asking variations of the same skill
        String skill = skillsList.get((order - 1) % skillsList.size());

        int catIdx = Math.max(0, Math.min(QUESTION_CATEGORIES.length - 1, order - 1));
        String category = QUESTION_CATEGORIES[catIdx];

        return switch (order) {
            case 1 -> new GeneratedQuestionDto(
                    String.format("To start our assessment, could you explain the internal execution and memory model of %s, including how resources are allocated and garbage collected?", skill),
                    category,
                    "Intermediate",
                    String.format("%s memory model, heap vs stack, garbage collection algorithms, lifecycle management", skill),
                    "Explains memory allocation, stack frames, heap references, and GC lifecycle correctly",
                    skill
            );
            case 2 -> new GeneratedQuestionDto(
                    String.format("In a practical enterprise application using %s, how do you implement robust error handling, transactional boundaries, and input validation?", skill),
                    category,
                    "Intermediate",
                    String.format("%s exception hierarchy, @Transactional propagation, validation annotations, rollback rules", skill),
                    "Addresses transactional isolation/propagation, global exception filters, and input constraints",
                    skill
            );
            case 3 -> new GeneratedQuestionDto(
                    String.format("When designing RESTful APIs or service integrations with %s, how do you enforce idempotent requests, versioning, and contract-first payload schemas?", skill),
                    category,
                    "Intermediate",
                    "HTTP idempotency tokens, URI/header versioning, OpenAPI/JSON schema contracts, error contracts",
                    "Explains idempotency keys for POST/PUT, backward compatibility, and schema validation",
                    skill
            );
            case 4 -> new GeneratedQuestionDto(
                    "How do you optimize slow database queries, configure connection pool limits, and resolve N+1 query bottlenecks in a high-concurrency environment?",
                    category,
                    "Advanced",
                    "B-tree indexing, query explain plans, HikariCP sizing, JOIN FETCH / batch fetching, isolation levels",
                    "Identifies N+1 problem causes, indexing strategies, and connection pool starvation mitigation",
                    "Database & SQL"
            );
            case 5 -> new GeneratedQuestionDto(
                    "On the client or integration layer, how do you manage reactive state streams, optimize client-side bundle size, and guard against cross-site scripting vulnerabilities?",
                    category,
                    "Intermediate",
                    "Reactive streams, state management (Signals/RxJS), code-splitting/lazy loading, DOM sanitization, CSP",
                    "Details memory leak prevention in subscriptions, chunk optimization, and XSS sanitization",
                    "Frontend Architecture"
            );
            case 6 -> new GeneratedQuestionDto(
                    String.format("Imagine you encounter a sudden latency spike or deadlock in a production %s service. What exact debugging tools and diagnostic steps would you use to isolate the root cause?", skill),
                    category,
                    "Advanced",
                    "Thread dumps, CPU profiling, heap analysis, APM metrics (p99 latency), database lock queries",
                    "Presents systematic triage: metric correlation, thread dump inspection, identifying contention/locks",
                    skill
            );
            case 7 -> new GeneratedQuestionDto(
                    "How do you design stateless token-based authentication with JWTs, handle seamless token rotation, and enforce fine-grained role-based authorization?",
                    category,
                    "Advanced",
                    "Access/refresh token lifecycles, cryptographically signed claims, token revocation, RBAC/ABAC guards",
                    "Explains silent refresh rotation, short TTLs, signature validation, and authorization filters",
                    "Security & Authentication"
            );
            case 8 -> new GeneratedQuestionDto(
                    "How do you manage concurrent thread execution safely, prevent race conditions or thread starvation, and choose between pessimistic and optimistic locking?",
                    category,
                    "Advanced",
                    "Thread safety, atomic primitives, volatile memory visibility, version columns vs row locks",
                    "Compares lock contention trade-offs: version-based optimistic retry vs database row-level locking",
                    "Concurrency & Threading"
            );
            case 9 -> new GeneratedQuestionDto(
                    String.format("When designing distributed services for %s, how do you maintain data consistency across microservices and implement distributed tracing?", jobTitle),
                    category,
                    "Advanced",
                    "Saga pattern, outbox pattern, OpenTelemetry / traceparent headers, eventual consistency",
                    "Explains compensation transactions, idempotent event publishing, and distributed trace context propagation",
                    "Microservices & System Design"
            );
            default -> new GeneratedQuestionDto(
                    "During a critical production outage with failing downstream dependencies, what circuit-breaking, fallback, and graceful degradation strategies would you apply?",
                    category,
                    "Advanced",
                    "Circuit breaker state transitions (closed/open/half-open), timeouts, cache fallbacks, bulkhead isolation",
                    "Describes fail-fast thresholds, non-blocking fallback responses, and system recovery mechanisms",
                    "Resilience & Production Ops"
            );
        };
    }

    private EvaluatedAnswerDto fallbackAnswerEvaluation(String answer, InterviewQuestion q) {
        if (answer == null || answer.trim().isEmpty() || answer.contains("[SKIPPED")) {
            return new EvaluatedAnswerDto(
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    "No spoken response detected within the 15-second window.",
                    false,
                    "INCORRECT",
                    "The question required explaining the underlying architecture and practical trade-offs.",
                    List.of("Conceptual definition", "Implementation approach"),
                    Collections.emptyList(),
                    List.of("Ensure speech is clear and begins within the active response window.")
            );
        }

        // Section 23A Rule: Never generate a fake score when evaluation fails
        log.warn("AI evaluation models were unreachable for question ID {}. Refusing to generate arbitrary fake score.",
                q != null ? q.getId() : "unknown");

        // Deterministic analysis based on keyword overlap with expected concepts
        String cleanAnswer = answer.trim().toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String expected = (q != null && q.getExpectedConcepts() != null) ? q.getExpectedConcepts().toLowerCase() : "";
        String[] rawKeywords = expected.split("[,;\\s]+");
        int matched = 0;
        int totalValidKeywords = 0;
        for (String kw : rawKeywords) {
            String cleanKw = kw.replaceAll("[^a-z0-9]", "").trim();
            if (cleanKw.length() >= 3) {
                totalValidKeywords++;
                if (cleanAnswer.contains(cleanKw)) {
                    matched++;
                }
            }
        }

        // Derive observable communication fluency purely from transcript characteristics:
        // Directness, word count, absence of repetitive fillers, coherent sentence structure
        int wordCount = cleanAnswer.split("\\s+").length;
        String[] fillers = {"um", "uh", "like", "you know", "i mean", "sort of", "kind of", "actually", "basically"};
        int fillerCount = 0;
        for (String f : fillers) {
            if (cleanAnswer.contains(f)) {
                fillerCount++;
            }
        }

        // Base fluency on speech volume and directness
        double baseFluency = Math.min(90.0, Math.max(30.0, wordCount * 2.5));
        double confidence = Math.max(10.0, Math.min(100.0, baseFluency - (fillerCount * 8.0)));

        if (totalValidKeywords > 0 && matched > 0) {
            double ratio = (double) matched / totalValidKeywords;
            double tech = Math.min(100.0, Math.round(ratio * 95.0 * 10.0) / 10.0);
            double clarity = Math.min(100.0, Math.round(Math.max(30.0, confidence * 0.9) * 10.0) / 10.0);
            double completeness = Math.min(100.0, Math.round(ratio * 90.0 * 10.0) / 10.0);
            double overall = Math.round(((tech * 0.45) + (completeness * 0.25) + (clarity * 0.15) + (confidence * 0.15)) * 10.0) / 10.0;
            boolean isCorrect = tech >= 60.0;
            String classification = tech >= 75.0 ? "CORRECT" : (tech >= 45.0 ? "PARTIALLY_CORRECT" : "INCORRECT");

            List<String> missing = new ArrayList<>();
            for (String kw : rawKeywords) {
                String cleanKw = kw.replaceAll("[^a-z0-9]", "").trim();
                if (cleanKw.length() >= 3 && !cleanAnswer.contains(cleanKw) && missing.size() < 3) {
                    missing.add(cleanKw);
                }
            }

            return new EvaluatedAnswerDto(
                    tech,
                    clarity,
                    completeness,
                    confidence,
                    overall,
                    classification.equals("CORRECT")
                            ? "Solid response addressing key technical mechanisms."
                            : (classification.equals("PARTIALLY_CORRECT")
                            ? "Partially addressed core concepts, but missed key mechanisms."
                            : "Response lacked depth on the expected technical mechanisms."),
                    isCorrect,
                    classification,
                    "Concept verified against expected technical criteria.",
                    missing,
                    List.of("Demonstrated familiarity with " + (q != null && q.getCategory() != null ? q.getCategory() : "the question topic")),
                    List.of("Elaborate further on concrete implementation details and edge cases")
            );
        }

        // If an answer was given but matched 0 technical concepts, it is INCORRECT with low scores (no fake 80)
        double zeroTech = 10.0;
        double zeroClarity = Math.min(60.0, confidence * 0.7);
        double zeroCompleteness = 5.0;
        double zeroOverall = Math.round(((zeroTech * 0.45) + (zeroCompleteness * 0.25) + (zeroClarity * 0.15) + (confidence * 0.15)) * 10.0) / 10.0;

        return new EvaluatedAnswerDto(
                zeroTech,
                zeroClarity,
                zeroCompleteness,
                confidence,
                zeroOverall,
                "Answer did not address the required technical concepts or mechanisms.",
                false,
                "INCORRECT",
                "The response diverged from the expected technical mechanisms and implementation principles.",
                List.of("Expected mechanisms: " + expected),
                Collections.emptyList(),
                List.of("Review foundational concepts for this topic")
        );
    }
}
