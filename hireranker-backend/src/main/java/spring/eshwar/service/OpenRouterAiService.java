package spring.eshwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spring.eshwar.exception.BadRequestException;
import spring.eshwar.service.ai.OpenRouterApiClient;

@Service
public class OpenRouterAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterAiService.class);

    private final OpenRouterApiClient openRouterApiClient;

    @Value("${application.ai.chatbot.model:${application.ai.openrouter.chatbot-model:${CHATBOT_AI_MODEL:nvidia/nemotron-3.5-lightning:free}}}")
    private String model;

    public OpenRouterAiService(OpenRouterApiClient openRouterApiClient) {
        this.openRouterApiClient = openRouterApiClient;
    }

    @Override
    public String generateResponse(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new BadRequestException("Prompt message cannot be blank.");
        }

        if (!openRouterApiClient.isConfigured()) {
            log.info("OpenRouter API key is not configured. Returning deterministic dynamic AI response.");
            return generateDynamicAiResponse(prompt.trim());
        }

        try {
            log.info("Sending prompt to OpenRouter model '{}'...", model);
            String result = openRouterApiClient.callChatCompletionWithFallback(
                    model,
                    "You are HireRanker AI Assistant. Provide direct, helpful, and concise answers.",
                    prompt.trim(),
                    false,
                    0.2,
                    300
            );

            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        } catch (Exception ex) {
            log.warn("OpenRouter call failed: {}. Falling back to dynamic simulated response.", ex.getMessage());
        }

        return generateDynamicAiResponse(prompt.trim());
    }

    @Override
    public String generateChatResponse(String message, String role, String userContext) {
        if (message == null || message.trim().isEmpty()) {
            throw new BadRequestException("Chat message cannot be blank.");
        }

        String roleStr = (role != null) ? role.toUpperCase() : "GENERAL";
        String systemInstruction = "ADMIN".equals(roleStr)
                ? "You are HireRanker AI Recruiter Copilot, an expert AI assistant for hiring managers. Assist with resume screening, evaluation criteria, candidate ranking, and interview questions."
                : "You are HireRanker AI Career Coach, an encouraging and expert AI assistant for job candidates. Help with resume enhancements, interview preparation, and technical career advice.";

        if (!openRouterApiClient.isConfigured()) {
            return generateFallbackChatResponse(message.trim(), roleStr);
        }

        String fullPrompt = String.format("""
                %s
                User Role: %s
                Context: %s
                Question: %s
                """, systemInstruction, roleStr, (userContext != null ? userContext : "None"), message.trim());

        try {
            return generateResponse(fullPrompt);
        } catch (Exception ex) {
            log.warn("AI chat call failed: {}. Falling back to simulated AI response.", ex.getMessage());
            return generateFallbackChatResponse(message.trim(), roleStr);
        }
    }

    private String generateFallbackChatResponse(String message, String role) {
        String lower = message.toLowerCase();

        if ("ADMIN".equals(role)) {
            if (lower.contains("criteria") || lower.contains("evaluation") || lower.contains("rank")) {
                return "💡 **Recruiter Tip - Evaluation Criteria & Ranking**:\n\n"
                        + "1. **Core Skills Match (40%)**: Set high weight on mandatory technologies.\n"
                        + "2. **Experience & Projects (30%)**: Check for production deployments and system architecture depth.\n"
                        + "3. **Live AI Interview Performance (30%)**: Utilize our Zero-Bias live interviewer for objective communication and problem-solving scoring.";
            } else if (lower.contains("question") || lower.contains("interview")) {
                return "📝 **AI Interview Questions Recommendation**:\n\n"
                        + "- **Technical Depth**: *How do you optimize slow database queries and handle distributed transactions in Spring Boot / Microservices?*\n"
                        + "- **Problem Solving**: *Walk through a critical production bug you solved and the root-cause analysis you conducted.*\n"
                        + "- **Architecture**: *How do you approach API rate-limiting and caching with Redis in high-concurrency environments?*";
            } else {
                return "🤖 **HireRanker Recruiter Copilot**:\n\n"
                        + "I can assist you with:\n"
                        + "• Setting up automated resume screening & semantic keyword matching\n"
                        + "• Generating tailored technical interview questions for candidates\n"
                        + "• Reviewing candidates' zero-bias live interview scorecards\n"
                        + "• Optimizing your job postings and required skill criteria.";
            }
        } else {
            // CANDIDATE
            if (lower.contains("resume") || lower.contains("cv")) {
                return "📄 **HireRanker Resume Optimization Tips**:\n\n"
                        + "1. **Include Target Keywords**: Ensure technologies matching the job description (e.g. Java, Spring Boot, React) appear in your skills and project bullet points.\n"
                        + "2. **Quantify Impact**: Use metrics like *'Improved API throughput by 35%'* or *'Reduced query latency from 800ms to 90ms'*.\n"
                        + "3. **Clean Layout**: Use standard section headers so our PDF AI parser extracts 100% of your experience accurately.";
            } else if (lower.contains("interview") || lower.contains("prepare") || lower.contains("question")) {
                return "🎤 **AI Live Interview Preparation Guide**:\n\n"
                        + "• **15-Second Rule**: When each question is presented, you have 15 seconds to begin speaking. Once you start speaking, you have plenty of time to finish your thought!\n"
                        + "• **STAR Methodology**: Structure scenario answers by Situation, Task, Action, and Result.\n"
                        + "• **Zero-Bias Scoring**: Our AI strictly scores your technical accuracy and communication clarity — your personal identity and background have zero impact.";
            } else {
                return "✨ **HireRanker Career Assistant**:\n\n"
                        + "I'm your 24/7 AI career copilot! I can help you:\n"
                        + "• Review and boost your resume match score\n"
                        + "• Practice answers for live AI technical & behavioral interviews\n"
                        + "• Identify high-demand skills for the jobs you're applying to\n"
                        + "• Understand your interview scorecard and improvement topics.";
            }
        }
    }

    private String generateDynamicAiResponse(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("resume") || lower.contains("cv") || lower.contains("upload")) {
            return "HireRanker Resume Assistant: Resumes must be uploaded in PDF format (under 10 MB). Our pipeline uses Apache PDFBox and semantic matching to evaluate technical competencies, production experience, and qualifications against open job criteria.";
        } else if (lower.contains("rank") || lower.contains("ranking") || lower.contains("score") || lower.contains("weight")) {
            return "HireRanker Ranking System: Candidates are evaluated on a 100-point composite model: Mandatory Skills Match (40%), Relevant Experience (30%), and Live AI Interview Score (30%). Candidates with scores >= 84% earn the Top Listed badge.";
        } else if (lower.contains("interview") || lower.contains("live") || lower.contains("15") || lower.contains("timer")) {
            return "HireRanker Live Interview: Candidates answer dynamic questions with a 15-second speech countdown timer per question. Speech pauses the timer to allow up to 2 minutes of response time; silence skips to the next question with zero-bias scoring.";
        } else if (lower.contains("shortlist")) {
            return "HireRanker Shortlisting: Recruiters can review top-ranked applicants and transition qualified candidates to SHORTLISTED status, unlocking automated interview scheduling.";
        } else if (lower.contains("spring") || lower.contains("boot") || lower.contains("injection") || lower.contains("ioc")) {
            return "In Spring Boot, Inversion of Control (IoC) and Dependency Injection (DI) decouple object creation from application logic, with Spring managing beans through constructor injection.";
        } else {
            return "HireRanker AI Assistant: Received query: \"" + prompt + "\". HireRanker offers automated ATS resume screening, objective candidate ranking, and zero-bias AI live interview assessments.";
        }
    }
}
