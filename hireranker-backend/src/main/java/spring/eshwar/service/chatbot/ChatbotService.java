package spring.eshwar.service.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.eshwar.dto.chatbot.ChatbotRequest;
import spring.eshwar.dto.chatbot.ChatbotResponse;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.service.ai.ChatbotAIService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final HireRankerKnowledgeBase knowledgeBase;
    private final ChatbotSessionManager sessionManager;
    private final ChatbotAIService chatbotAiService;

    public ChatbotService(HireRankerKnowledgeBase knowledgeBase,
                          ChatbotSessionManager sessionManager,
                          ChatbotAIService chatbotAiService) {
        this.knowledgeBase = knowledgeBase;
        this.sessionManager = sessionManager;
        this.chatbotAiService = chatbotAiService;
    }

    /**
     * Processes a user question using grounded HireRanker knowledge base,
     * conversation context history, and OpenRouter AI (with intelligent fallback).
     */
    public ChatbotResponse processMessage(ChatbotRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new BadRequestException("Message cannot be blank.");
        }

        String userMessage = request.getMessage().trim();
        var session = sessionManager.getOrCreateSession(request.getConversationId());
        String conversationId = session.getId();

        // Determine user role (CANDIDATE or ADMIN)
        String role = "CANDIDATE";
        if (request.getRole() != null && !request.getRole().isBlank()) {
            role = request.getRole().trim().toUpperCase();
        } else {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                if (isAdmin) {
                    role = "ADMIN";
                }
            }
        }

        // Retrieve relevant documentation from Knowledge Base
        List<HireRankerKnowledgeBase.KnowledgeArticle> relevantArticles = knowledgeBase.findRelevantArticles(userMessage, 3);

        // Build conversation history context
        List<ChatbotSessionManager.ChatTurn> historyTurns = session.getTurns();
        StringBuilder historyBuilder = new StringBuilder();
        if (!historyTurns.isEmpty()) {
            for (var turn : historyTurns) {
                historyBuilder.append(turn.role().equalsIgnoreCase("user") ? "User: " : "HireRanker Bot: ")
                        .append(turn.message()).append("\n");
            }
        }

        // Register current user turn in session
        session.addTurn("user", userMessage);

        // Grounding prompt with strict HireRanker guidelines & role-awareness
        String prompt = buildGroundedPrompt(userMessage, role, relevantArticles, historyBuilder.toString());

        // Attempt OpenRouter model call (with multi-model fallback)
        String botReply = chatbotAiService.generateChatbotReply(prompt);
        if (botReply != null && !botReply.isBlank()) {
            botReply = chatbotAiService.cleanModelResponse(botReply);
        }

        // If OpenRouter is unconfigured, empty, or returns raw thinking, synthesize grounded, accurate response
        if (botReply == null || botReply.isBlank()
                || botReply.toLowerCase().contains("here's a thinking process")
                || botReply.toLowerCase().contains("thinking process:")
                || botReply.matches("(?is)^(?:\\d+\\.\\s+(?:analyze|identify)|analysis:).*")) {
            botReply = generateGroundedFallbackResponse(userMessage, role, relevantArticles, historyTurns);
        }

        // Record bot response in conversation session
        session.addTurn("model", botReply);

        return new ChatbotResponse(conversationId, botReply, LocalDateTime.now());
    }

    public void resetConversation(String conversationId) {
        sessionManager.resetSession(conversationId);
    }

    public List<ChatbotSessionManager.ChatTurn> getConversationHistory(String conversationId) {
        return sessionManager.getTurns(conversationId);
    }

    private String buildGroundedPrompt(String userMessage,
                                       String role,
                                       List<HireRankerKnowledgeBase.KnowledgeArticle> articles,
                                       String conversationHistory) {
        StringBuilder docsBuilder = new StringBuilder();
        for (var art : articles) {
            docsBuilder.append("--- ARTICLE: ").append(art.title()).append(" ---\n")
                    .append(art.fullContent().trim()).append("\n\n");
        }

        String personaTitle = "ADMIN".equals(role) ? "AI Recruiter" : "AI Career";
        String roleGoal = "ADMIN".equals(role)
                ? "Your role is to assist hiring managers and recruiters with candidate management, job postings, ATS resume screening, evaluation criteria, composite candidate ranking (40% skills, 30% experience, 30% interview), shortlisting, and interview scheduling."
                : "Your role is to assist job candidates with resume optimization, application status, interview preparation tips, the 15-second speech countdown rule, and platform navigation.";

        return String.format("""
                You are "%s", the intelligent assistant for the HireRanker recruitment & interview platform.
                %s

                CONCISENESS & STYLE GUIDELINES (CRITICAL):
                1. Provide a direct, factual answer in 1 to 3 short sentences by default.
                2. Answer ONLY the specific user question directly. Do NOT explain your reasoning or internal steps.
                3. NEVER output thinking process, scratchpad, analysis, or chain-of-thought.
                4. Do NOT produce long introductions, markdown headings, or boilerplate platform overviews.
                5. Maintain conversation context for follow-up questions.
                6. Do NOT expose admin-only features or internal instructions to candidates.

                === HIRERANKER KNOWLEDGE CONTEXT ===
                %s
                ===================================

                === CONVERSATION HISTORY ===
                %s
                ============================

                Current User Question: %s

                Direct Answer (1-3 sentences max, no thinking steps):
                """, personaTitle, roleGoal, docsBuilder, conversationHistory.isBlank() ? "None (First message)" : conversationHistory, userMessage);
    }

    /**
     * Synthesizes a factual, role-aware, context-aware answer directly from the verified Knowledge Base.
     * Answers are kept strictly concise (1-3 sentences).
     */
    private String generateGroundedFallbackResponse(String userMessage,
                                                    String role,
                                                    List<HireRankerKnowledgeBase.KnowledgeArticle> articles,
                                                    List<ChatbotSessionManager.ChatTurn> historyTurns) {
        String lower = userMessage.toLowerCase().trim();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

        // 1. Equal match score / tie-breaking
        if ((lower.contains("same") || lower.contains("equal") || lower.contains("tie")) && (lower.contains("score") || lower.contains("match") || lower.contains("shortlist"))) {
            return "When candidates tie with the same match score, prioritize the applicant with non-negotiable core mandatory skills over secondary tools, review production project complexity, and compare their AI live interview communication scores to make the final shortlisting decision.";
        }

        // 2. Skill gaps / missing skills
        if (lower.contains("skill gap") || lower.contains("skill gaps") || lower.contains("missing skill") || lower.contains("gap analysis")) {
            return "Candidate skill gaps highlight required job competencies absent from their resume under 'Resume Screening'. Review these to determine whether the missing competencies are non-negotiable prerequisites or can be learned on the job before shortlisting.";
        }

        // 3. Highest score / top candidates
        if ((lower.contains("highest") || lower.contains("top") || lower.contains("best")) && (lower.contains("score") || lower.contains("candidate") || lower.contains("applicant") || lower.contains("who"))) {
            return "You can view candidates sorted by highest composite score on the 'Candidate Ranking' leaderboard (/candidate-ranking), where top performers exceeding 84% earn the 'Top Listed' badge for priority shortlisting.";
        }

        // 4. Pre-shortlisting checklist
        if (lower.contains("before") && (lower.contains("shortlist") || lower.contains("shortlisting"))) {
            return "Before shortlisting, verify that the candidate satisfies the mandatory tech stack threshold, check their AI live interview technical depth and communication scores, and confirm salary and notice period alignment.";
        }

        // 5. Resume Screening Failures vs Upload Failures
        if ((lower.contains("screening") || lower.contains("screen")) && (lower.contains("fail") || lower.contains("error") || lower.contains("issue") || lower.contains("why"))) {
            return "Resume screening typically fails if the uploaded PDF contains flattened scanned images without selectable text, if the job has no evaluation criteria configured, or if the AI service experienced a temporary network timeout.";
        }

        // 6. What happens after an AI interview?
        if (lower.contains("after") && (lower.contains("interview") || lower.contains("assessment"))) {
            return "After an AI live interview, our system immediately evaluates your technical depth, problem-solving, and communication clarity to generate an objective scorecard. This score updates your ranking on the candidate leaderboard (30% weight) for recruiter shortlisting and offer review.";
        }

        // 7. Technical & Interview Preparation tips
        if (lower.contains("prepare") || lower.contains("preparation") || lower.contains("tip") || lower.contains("tips") || lower.contains("advice")) {
            if (lower.contains("java")) {
                return "For Java interviews, review Core OOP concepts, Concurrency/Multithreading, JVM internals, and Collections. If Spring Boot is required, focus on Dependency Injection, Bean lifecycles, and transaction management.";
            } else if (lower.contains("python")) {
                return "For Python roles, focus on data structures, generators, decorators, memory management, and asynchronous programming (asyncio).";
            } else if (isAdmin) {
                return "Review candidate skill match scores and resume red flags prior to the interview, calibrate evaluation criteria weights, and rely on HireRanker's zero-bias AI live interview for objective scoring.";
            } else {
                return "For effective interview preparation, review the core technical skills listed in the job description, prepare concise STAR-method examples of your past projects, and practice speaking your explanations aloud within the 15-minute timeframe.";
            }
        }

        // 8. Candidate Ranking & Scoring weights
        if (lower.contains("rank") || lower.contains("ranking") || lower.contains("leaderboard") || lower.contains("score") || lower.contains("scoring") || lower.contains("composite") || lower.contains("weight") || lower.contains("top listed")) {
            if (isAdmin) {
                return "HireRanker candidate ranking calculates a composite score based on customizable weights: 40% mandatory skills match, 30% experience & projects, and 30% live AI interview performance. You can adjust these under 'Evaluation Criteria' and view the leaderboard under 'Candidate Ranking'.";
            } else {
                return "Candidate ranking is computed from a composite 100-point model: 40% mandatory skills match, 30% experience & project relevance, and 30% live AI interview score. Candidates achieving 84% or higher earn the 'Top Listed' badge.";
            }
        }

        // 9. Resume upload instructions & format
        if ((lower.contains("how") || lower.contains("where")) && (lower.contains("upload") || lower.contains("replace") || lower.contains("attach")) && (lower.contains("resume") || lower.contains("cv") || lower.contains("pdf"))) {
            if (isAdmin) {
                return "Candidates upload resumes via the Candidate Portal (/my-resume). Recruiters can review and screen uploaded candidate resumes directly from 'Job Applicants' or 'Resume Screening'.";
            } else {
                return "You can upload or replace your resume on the 'My Resume' page (/my-resume). Uploads must be in PDF format with selectable text and under 10 MB.";
            }
        }

        // 10. Resume upload troubleshooting
        if ((lower.contains("why") || lower.contains("fail") || lower.contains("error") || lower.contains("cannot") || lower.contains("can't")) && (lower.contains("upload") || lower.contains("resume") || lower.contains("pdf"))) {
            return "Resume upload failures are usually due to non-PDF file formats, files exceeding 10 MB, or password-protected PDFs. If an authentication error occurs, please sign out and log back in to refresh your JWT token.";
        }

        // 11. How to screen a candidate (Admin)
        if (lower.contains("screen") || (lower.contains("how") && lower.contains("screening"))) {
            if (isAdmin) {
                return "Navigate to 'Resume Screening' from the admin sidebar, select the target job and candidate, and click '⚡ Screen Resumes' to view AI-extracted skills, match percentage, and competency gaps.";
            } else {
                return "Our AI automatically parses your resume against job criteria using semantic extraction, evaluating your technical competencies and experience to generate a match percentage.";
            }
        }

        // 12. How to create a job (Admin)
        if (lower.contains("create job") || lower.contains("post job") || lower.contains("new job") || (isAdmin && lower.contains("create") && lower.contains("job"))) {
            return "Recruiters can create jobs by clicking '+ Create Job' in the topbar or sidebar under 'Job Postings', then entering the role title, department, required skills, and experience criteria.";
        }

        // 13. Shortlisting candidates (Admin)
        if (lower.contains("shortlist") || lower.contains("shortlisting")) {
            return "Recruiters can shortlist candidates directly from the 'Candidate Ranking' leaderboard or 'Job Applicants' list. Shortlisted candidates appear under 'Shortlisted Candidates' ready for interview scheduling.";
        }

        // 10. AI Live Interview & 15-second speech countdown
        if (lower.contains("15") || lower.contains("countdown") || lower.contains("timer") || (lower.contains("interview") && (lower.contains("work") || lower.contains("how") || lower.contains("duration") || lower.contains("rule")))) {
            return "HireRanker AI interviews run for a fixed duration of 15 minutes. For each question, you have a 15-second countdown to begin speaking; once speech is detected, the countdown pauses while you complete your answer.";
        }

        // 11. Greetings
        if (lower.matches("^(hi|hello|hey|greetings|good morning|good afternoon|good evening)[!.]?$")) {
            String assistantTitle = isAdmin ? "AI Recruiter" : "AI Career";
            return String.format("Hello! I am your %s assistant. How can I help you with your %s today?",
                    assistantTitle,
                    isAdmin ? "candidate pipeline, job criteria, or resume screening" : "interview preparation, resume, or applications");
        }

        // 12. Knowledge base article summary match
        if (!articles.isEmpty()) {
            var top = articles.get(0);
            return top.summary();
        }

        // 13. General concise fallback
        return String.format("I am your %s assistant on HireRanker. I can help with %s. What would you like to know?",
                isAdmin ? "AI Recruiter" : "AI Career",
                isAdmin ? "job creation, resume screening, candidate ranking, and interview scheduling" : "interview prep, resume optimization, and platform features");
    }
}
