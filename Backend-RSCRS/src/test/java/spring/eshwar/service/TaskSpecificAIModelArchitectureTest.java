package spring.eshwar.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.eshwar.dto.chatbot.ChatbotRequest;
import spring.eshwar.dto.chatbot.ChatbotResponse;
import spring.eshwar.dto.interview.StructuredCandidateProfileDto;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.InterviewQuestion;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.Resume;
import spring.eshwar.service.AiInterviewService.GeneratedInterviewResultDto;
import spring.eshwar.service.AiInterviewService.GeneratedQuestionDto;
import spring.eshwar.service.ai.AIScreeningProvider.ScreeningContext;
import spring.eshwar.service.ai.AIScreeningProvider.ScreeningEvaluationResult;
import spring.eshwar.service.ai.ChatbotAIService;
import spring.eshwar.service.ai.GroqApiClient;
import spring.eshwar.service.ai.OpenRouterApiClient;
import spring.eshwar.service.ai.ResumeScreeningAIService;
import spring.eshwar.service.chatbot.ChatbotService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TaskSpecificAIModelArchitectureTest {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ChatbotAIService chatbotAiService;

    @Autowired
    private ResumeScreeningAIService screeningAiService;

    @Autowired
    private AiInterviewService aiInterviewService;

    @Autowired
    private GroqApiClient groqApiClient;

    @Autowired
    private OpenRouterApiClient openRouterApiClient;

    @Test
    @DisplayName("OpenRouter Architecture - Configured task-specific model IDs")
    void testOpenRouterModelArchitecture() {
        assertThat(chatbotAiService.getChatbotModel()).isEqualTo("qwen/qwen3-8b:free");
        assertThat(screeningAiService.getScreeningModel()).isEqualTo("qwen/qwen3-30b-a3b:free");
        assertThat(openRouterApiClient.getFallbackModel()).isEqualTo("openrouter/free");
    }

    @Test
    @DisplayName("AI Chatbot - Answers platform navigation questions")
    void testChatbotNavigationQuery() {
        ChatbotRequest request = new ChatbotRequest();
        request.setMessage("How do I view candidate rankings and match scores?");

        ChatbotResponse response = chatbotService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getResponse()).isNotBlank();
        assertThat(response.getConversationId()).isNotBlank();
        String lower = response.getResponse().toLowerCase();
        assertThat(lower).containsAnyOf("ranking", "score", "candidate", "match");
    }

    @Test
    @DisplayName("AI Chatbot - Multi-turn conversation context is preserved")
    void testChatbotMultiTurnContext() {
        ChatbotRequest req1 = new ChatbotRequest();
        req1.setMessage("What are the interview rules?");
        ChatbotResponse res1 = chatbotService.processMessage(req1);

        assertThat(res1.getConversationId()).isNotBlank();

        ChatbotRequest req2 = new ChatbotRequest();
        req2.setConversationId(res1.getConversationId());
        req2.setMessage("Can you explain the 15-second countdown rule in detail?");
        ChatbotResponse res2 = chatbotService.processMessage(req2);

        assertThat(res2.getConversationId()).isEqualTo(res1.getConversationId());
        assertThat(res2.getResponse().toLowerCase()).containsAnyOf("15", "second", "countdown", "speech");
    }

    @Test
    @DisplayName("AI Resume Screening - Evaluates candidate profile and produces structured result")
    void testResumeScreeningStructuredEvaluation() {
        ScreeningContext context = new ScreeningContext(
                "Senior Full-Stack Java Engineer",
                "Build resilient microservices using Spring Boot, JPA, MySQL, and Angular frontend.",
                "Java, Spring Boot, Angular, MySQL, Docker, REST API",
                "4 years",
                3.0,
                "B.Tech / B.E.",
                40.0,
                30.0,
                10.0,
                "Eshwar - Software Engineer. 5 years experience building Spring Boot microservices, REST APIs, Angular single-page apps, and MySQL databases. Extensive experience with Docker containerization."
        );

        ScreeningEvaluationResult result = screeningAiService.evaluateResume(context);

        assertThat(result).isNotNull();
        assertThat(result.overallScore()).isNotNull();
        assertThat(result.overallScore()).isBetween(0.0, 100.0);
        assertThat(result.matchingSkills()).isNotBlank();
        assertThat(result.matchingSkills().toLowerCase()).containsAnyOf("java", "spring", "angular");
        assertThat(result.recommendation()).isNotNull();
    }

    @Test
    @DisplayName("AI Live Interview - Extracts structured candidate profile and generates adaptive question")
    void testInterviewAdaptiveQuestionGeneration() {
        Candidate candidate = new Candidate();
        candidate.setFullName("Alex Rivera");
        candidate.setSkills("Java, Spring Boot, Microservices, Angular, PostgreSQL");
        candidate.setExperience("4 years building cloud-native banking microservices");
        candidate.setEducation("B.S. in Computer Science");

        Resume resume = new Resume();
        resume.setExtractedText("Alex Rivera - Experienced backend engineer specializing in Spring Boot distributed transaction processing. Skills: Java, Spring Boot, Redis, Kafka.");

        Job job = new Job();
        job.setTitle("Lead Backend Developer");
        job.setRequiredSkills("Java 21, Spring Boot 3, Distributed Systems, High Concurrency");
        job.setExperienceRequired("4 years");

        StructuredCandidateProfileDto profile = StructuredCandidateProfileDto.fromCandidateAndResume(candidate, resume);

        assertThat(profile).isNotNull();
        assertThat(profile.candidateName()).isEqualTo("Alex Rivera");
        assertThat(profile.coreSkills()).contains("Java");

        GeneratedQuestionDto genQ = aiInterviewService.generateNextQuestion(
                candidate,
                resume,
                job,
                List.of(),
                List.of(),
                1
        );

        assertThat(genQ).isNotNull();
        assertThat(genQ.questionText()).isNotBlank();
        assertThat(genQ.category()).isNotBlank();
        assertThat(genQ.difficulty()).isNotBlank();
    }

    @Test
    @DisplayName("AI Live Interview - Generates objective zero-bias scorecard")
    void testInterviewScorecardGeneration() {
        Job job = new Job();
        job.setTitle("Senior Cloud Engineer");
        job.setRequiredSkills("AWS, Kubernetes, Terraform");

        Candidate candidate = new Candidate();
        candidate.setFullName("Morgan Lee");
        candidate.setSkills("AWS, Kubernetes, Docker, Terraform");
        candidate.setExperience("5 years");
        candidate.setEducation("M.S. Computer Science");

        GeneratedInterviewResultDto result = aiInterviewService.synthesizeFinalResult(
                candidate,
                job,
                List.of(),
                List.of(),
                15.0
        );

        assertThat(result).isNotNull();
        assertThat(result.overallScore()).isBetween(0.0, 100.0);
        assertThat(result.technicalScore()).isBetween(0.0, 100.0);
        assertThat(result.communicationScore()).isBetween(0.0, 100.0);
        assertThat(result.recommendation()).isNotBlank();
        assertThat(result.strengths()).isNotBlank();
    }

    @Test
    @DisplayName("Groq API Client - Gracefully handles unconfigured key without crashing")
    void testGroqClientFallbackWhenNotConfigured() {
        boolean configured = groqApiClient.isConfigured();
        if (!configured) {
            String result = groqApiClient.callChatCompletion("System", "User prompt", false);
            assertThat(result).isNull();
        }
    }

    @Test
    @DisplayName("AI Live Interview - Evaluate Answer verifies 4 distinct grading cases without fake scores")
    void testInterviewAnswerEvaluationFourCases() {
        InterviewQuestion question = new InterviewQuestion();
        question.setQuestionText("Explain how Spring Boot handles dependency injection and the role of @Autowired.");
        question.setCategory("Spring Boot");
        question.setDifficulty("MEDIUM");
        question.setExpectedConcepts("Dependency Injection, Inversion of Control (IoC), ApplicationContext, @Autowired, Bean wiring");
        question.setKeyPoints("Spring IoC container injects dependent beans automatically; @Autowired injects by type; constructor injection is preferred.");

        Job job = new Job();
        job.setTitle("Senior Java Developer");
        job.setRequiredSkills("Java, Spring Boot, Microservices");

        // Case 1: Empty / Skipped answer -> Must return 0 across all scores and INCORRECT classification
        AiInterviewService.EvaluatedAnswerDto emptyResult = aiInterviewService.evaluateAnswer(
                question, "", 1.0, 0.0, job);
        assertThat(emptyResult).isNotNull();
        assertThat(emptyResult.technicalScore()).isEqualTo(0.0);
        assertThat(emptyResult.completenessScore()).isEqualTo(0.0);
        assertThat(emptyResult.clarityScore()).isEqualTo(0.0);
        assertThat(emptyResult.confidenceScore()).isEqualTo(0.0);
        assertThat(emptyResult.overallScore()).isEqualTo(0.0);
        assertThat(emptyResult.correctnessClassification()).isEqualTo("INCORRECT");
        assertThat(emptyResult.correct()).isFalse();

        // Case 2: Full / Strong answer matching expected concepts
        String strongAnswer = "Spring Boot manages Dependency Injection using the IoC container and ApplicationContext. Beans are configured and wired automatically. @Autowired resolves and injects dependencies into components, usually via constructor injection.";
        AiInterviewService.EvaluatedAnswerDto strongResult = aiInterviewService.evaluateAnswer(
                question, strongAnswer, 2.0, 15.0, job);
        assertThat(strongResult).isNotNull();
        assertThat(strongResult.technicalScore()).isGreaterThan(50.0);
        assertThat(strongResult.overallScore()).isGreaterThan(50.0);
        assertThat(strongResult.correctnessClassification()).isIn("CORRECT", "PARTIALLY_CORRECT");

        // Case 3: Partially correct answer (only mentions @Autowired annotation briefly)
        String partialAnswer = "In Spring we can use @Autowired to connect things together.";
        AiInterviewService.EvaluatedAnswerDto partialResult = aiInterviewService.evaluateAnswer(
                question, partialAnswer, 3.0, 6.0, job);
        assertThat(partialResult).isNotNull();
        assertThat(partialResult.technicalScore()).isLessThanOrEqualTo(strongResult.technicalScore());
        assertThat(partialResult.correctnessClassification()).isIn("PARTIALLY_CORRECT", "INCORRECT");

        // Case 4: Completely incorrect / off-topic answer
        String wrongAnswer = "Photosynthesis is the process where plants convert sunlight into chemical energy.";
        AiInterviewService.EvaluatedAnswerDto wrongResult = aiInterviewService.evaluateAnswer(
                question, wrongAnswer, 2.0, 8.0, job);
        assertThat(wrongResult).isNotNull();
        assertThat(wrongResult.technicalScore()).isLessThanOrEqualTo(30.0);
        assertThat(wrongResult.overallScore()).isLessThanOrEqualTo(35.0);
        assertThat(wrongResult.correctnessClassification()).isEqualTo("INCORRECT");
        assertThat(wrongResult.correct()).isFalse();
    }
}