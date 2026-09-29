package spring.eshwar.service.chatbot;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HireRankerKnowledgeBase {

    public record KnowledgeArticle(
            String id,
            String title,
            String category,
            List<String> keywords,
            String summary,
            String fullContent
    ) {}

    private final List<KnowledgeArticle> articles = new ArrayList<>();

    public HireRankerKnowledgeBase() {
        initializeKnowledgeBase();
    }

    private void initializeKnowledgeBase() {
        articles.add(new KnowledgeArticle(
                "kb-resume-upload",
                "How to Upload a Resume in HireRanker",
                "Resume",
                Arrays.asList("resume", "upload", "cv", "pdf", "file", "format", "profile", "attach"),
                "Upload your resume in PDF format (up to 10MB) via the Candidate Portal.",
                """
                ### How to Upload Your Resume:
                1. Log in to the Candidate Portal.
                2. Navigate to "My Resume" (`/my-resume`) or click your profile pill in the sidebar.
                3. Drag and drop your resume file or click "Browse Files".
                4. Supported format: **PDF only** (`.pdf`). Word documents (.docx) and images are not supported.
                5. Maximum file size: **10 MB**.
                6. Upon upload, Apache PDFBox extracts your skills, experience, and education to match you against open jobs.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-resume-troubleshooting",
                "Troubleshooting Resume Upload Problems",
                "Troubleshooting",
                Arrays.asList("upload", "error", "fail", "cannot upload", "can't upload", "rejected", "stuck", "invalid file", "resume problem"),
                "Resolve common resume upload errors such as wrong file format or size limits.",
                """
                ### Why Can't I Upload My Resume? (Troubleshooting Checklist):
                1. **File Format Check**: HireRanker strictly accepts **PDF files** (`.pdf`). If your resume is in Microsoft Word (.docx), Pages, or image format (JPG/PNG), please export/save it as a PDF before uploading.
                2. **File Size Limit**: The maximum allowed resume size is **10 MB**. Compress large PDFs or remove heavy embedded images.
                3. **Text Selectability**: Ensure your PDF contains selectable text. Scanned document images without OCR cannot be parsed by Apache PDFBox.
                4. **Candidate Authentication**: Confirm you are logged in as a candidate. If your session expired (24-hour JWT token expiration), log out and log in again at `/candidate-login`.
                5. **Required Profile**: Ensure your basic Candidate Profile (Name and Email) is saved before uploading.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-job-creation",
                "How to Create and Manage Job Postings",
                "Jobs",
                Arrays.asList("job", "posting", "create job", "post", "add job", "recruiter", "hiring", "description"),
                "Recruiters can create new job openings from the Recruiter Command Center or Job Postings page.",
                """
                ### How to Create a Job Posting:
                1. Log in to the Admin/Recruiter Portal.
                2. In the sidebar, click "Job Postings" (`/job-postings`) or click the "+ Create Job" button in the Topbar.
                3. Fill in the required fields:
                   - **Job Title**: e.g., Senior Full Stack Java Engineer
                   - **Company Name**: e.g., HireRanker Labs
                   - **Job Description**: Core responsibilities and daily impact
                   - **Required Skills**: Comma-separated list (e.g., Java, Spring Boot, React, SQL)
                   - **Experience Required**: e.g., 3-5 years
                   - **Location**: Remote, Hybrid, or City
                   - **Salary Range & Employment Type**: e.g., Full-time
                4. Click "Publish Job" to make it live for candidates to discover on `/find-jobs`.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-resume-screening",
                "How AI Resume Screening Works",
                "Screening",
                Arrays.asList("screening", "screen", "parse", "match", "score", "algorithm", "keywords", "ats"),
                "HireRanker compares extracted resume skills and experience against job criteria using semantic analysis.",
                """
                ### How Resume Screening Works:
                1. Navigate to "Resume Screening" (`/resume-screening`) in the Recruiter menu.
                2. HireRanker's AI pipeline extracts text from uploaded candidate resumes using Apache PDFBox.
                3. The system performs semantic skill extraction, identifying languages, frameworks, databases, and years of experience.
                4. The algorithm compares the candidate profile against the target Job Description and Required Skills.
                5. A Match Score (0 - 100%) is computed, and applicants are organized into:
                   - **Shortlisted** (High match, typically >= 80%)
                   - **Under Review** (Moderate match, 60% - 79%)
                   - **Rejected** (Below threshold, < 60%)
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-evaluation-criteria",
                "Evaluation Criteria & Weights",
                "Criteria",
                Arrays.asList("criteria", "weight", "weights", "evaluation", "percentage", "threshold"),
                "Default scoring weights: Skills Match (40%), Experience (30%), Interview Performance (30%).",
                """
                ### Evaluation Criteria & Weights in HireRanker:
                Recruiters can customize scoring rules on `/evaluation-criteria`:
                - **Skills Match (40% default weight)**: Evaluates overlap of mandatory and nice-to-have technologies.
                - **Experience & Projects (30% default weight)**: Evaluates years in the industry, seniority, and production project complexity.
                - **AI Live Interview Performance (30% default weight)**: Evaluates live technical accuracy and communication clarity.
                - **Minimum Shortlist Threshold**: Default recommendation is 75% - 80% to auto-qualify top candidates.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-candidate-ranking",
                "How Candidate Ranking & Shortlisting Works",
                "Ranking",
                Arrays.asList("ranking", "rank", "shortlist", "top", "leaderboard", "candidates", "shortlisted"),
                "Candidates are sorted by total composite match score on the Candidate Ranking leaderboard.",
                """
                ### Candidate Ranking & Shortlisting:
                1. Navigate to "Candidate Ranking" (`/candidate-ranking`) or view the dashboard leaderboard.
                2. Candidates are ranked in descending order based on their composite match score.
                3. **Tabs**:
                   - **All Candidates**: Displays the full pipeline.
                   - **Top Ranked**: Filters candidates with match score >= 84% or already marked Shortlisted.
                4. **Shortlisting**:
                   - Click "⭐ Shortlist" on any candidate row to move them into the Shortlisted pipeline (`/shortlisted-candidates`).
                   - Click "📅 Schedule Interview" to book an interview slot.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-tie-breaking",
                "How to Shortlist When Candidates Have the Same Match Score",
                "Shortlisting",
                Arrays.asList("tie", "same score", "equal score", "same match score", "tie breaker", "tie-breaking", "shortlist", "both candidates", "break tie"),
                "Break equal match score ties by evaluating core mandatory competencies, hands-on production project complexity, and AI live interview communication scores.",
                """
                ### How to Shortlist When Candidates Have Equal Match Scores:
                When two candidates have the same match score:
                1. Differentiate by core mandatory skills: Check which applicant possesses the non-negotiable architectural or framework requirements versus nice-to-have tools.
                2. Evaluate practical project depth: Examine verifiable project code, architectural ownership, and complexity of problems solved.
                3. Compare AI Live Interview scores: Review communication clarity, technical depth, and problem-solving metrics on their scorecards.
                4. You can also calibrate criteria weights on `/evaluation-criteria` to adjust skill versus experience priority, or invite both for a focused human interview round.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-skill-gaps",
                "Understanding and Reviewing Candidate Skill Gaps",
                "Screening",
                Arrays.asList("skill gap", "skill gaps", "missing skills", "competency gap", "missing", "gap analysis", "weaknesses"),
                "Skill gaps highlight required job competencies absent from a candidate's resume, visible under Resume Screening and candidate scorecards.",
                """
                ### Candidate Skill Gaps:
                1. Navigate to "Resume Screening" (`/resume-screening`) to inspect "Detected Skills", "Missing Skills", and "Recommended Skills".
                2. Missing skills pinpoint mandatory requirements from the job posting that were not found in the applicant's resume.
                3. Recruiters can evaluate whether the missing skill is a strict prerequisite or can be rapidly acquired on the job before making a shortlist decision.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-shortlist-criteria",
                "Pre-Shortlisting Evaluation Checklist for Recruiters",
                "Shortlisting",
                Arrays.asList("check before shortlisting", "shortlist criteria", "shortlisting checklist", "what to check", "shortlisting factors", "highest score", "who has the highest score"),
                "Before shortlisting, verify candidate mandatory skill match percentage, live interview scorecard, project portfolio relevance, and salary alignment.",
                """
                ### Pre-Shortlisting Checklist:
                1. Mandatory Skills Match: Ensure required tech stack overlap meets the minimum threshold (default 75%-80%).
                2. AI Live Interview Scorecard: Review technical depth, communication, and recommendation badges on the Candidate Ranking leaderboard.
                3. Practical Project Experience: Check project relevance, recency of technology usage, and years of experience.
                4. Availability & Fit: Confirm notice period, salary alignment, and location preferences before issuing an interview invite.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-ai-live-interview",
                "How the AI Live Interview Works (15-Second Rule & Zero-Bias)",
                "Interviews",
                Arrays.asList("interview", "live interview", "ai interview", "15-second", "15 second", "seconds", "camera", "microphone", "voice", "bias", "scorecard"),
                "10-15 minute live adaptive interview with 15-second speech rule and zero-bias scoring.",
                """
                ### AI Live Interviewer Overview:
                Candidates take an interactive 10–15 minute interview on `/interviews`:
                1. **Adaptive Questioning**: The AI dynamically generates 6 tailored questions (Resume-based, Technical Deep Dive, Project Experience, Scenario/Architecture, Job-Specific, Follow-up).
                2. **The 15-Second Rule**:
                   - After each question is read, a 15-second countdown begins.
                   - The candidate must **start speaking within 15 seconds**.
                   - Once speech is detected, the countdown timer pauses, allowing the candidate up to 2 minutes to explain their answer.
                   - If no speech is detected within 15 seconds, the question is marked `SKIPPED` with a 0 score and the next question loads.
                3. **Zero-Bias Policy**:
                   - The AI strictly evaluates technical correctness, problem-solving reasoning, and communication clarity.
                   - Physical appearance, demographic factors, gender, and regional accents are never scored.
                4. **Scorecard Output**:
                   - Generates Technical Score, Communication Score, Problem Solving Score, Strengths, Weaknesses, Improvement Topics, and Recommendation (`STRONG_HIRE`, `HIRE`, `CONSIDER`, `DO_NOT_HIRE`).
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-interview-scheduling",
                "Scheduling Human Recruiter Interviews",
                "Interviews",
                Arrays.asList("schedule", "interview scheduler", "meeting", "calendar", "google meet", "zoom", "link"),
                "Schedule recruiter interviews on /interview-scheduler with date, time, and meeting link.",
                """
                ### How to Schedule an Interview (Recruiter):
                1. Navigate to "Interview Scheduler" (`/interview-scheduler`) in the Recruiter menu.
                2. Select the applicant and target job position.
                3. Pick the Scheduled Date and Time.
                4. Enter the Interview Type (e.g. Technical Round, Cultural Fit, Final Round).
                5. Provide the Meeting Link (Google Meet, Zoom, Microsoft Teams) and preparation notes.
                6. Click "Schedule Interview". The applicant receives the status update in their "My Applications" and "Interviews" views.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-authentication-login",
                "Authentication, Roles & Login Troubleshooting",
                "Auth",
                Arrays.asList("login", "signin", "register", "signup", "password", "role", "jwt", "unauthorized", "token", "error 401"),
                "HireRanker supports Candidate and Admin roles secured with BCrypt and JWT tokens.",
                """
                ### Login & Authentication Guide:
                - **Candidate Login**: `/candidate-login` (requires candidate email and password).
                - **Candidate Registration**: `/candidate-register` (name, email, password, phone, location).
                - **Admin/Recruiter Portal**: Home page `/` or `/register`.
                - **JWT Security**: Sessions use 24-hour Bearer tokens.
                - **Troubleshooting 401 Unauthorized**:
                  If you receive 401 Unauthorized errors when navigating or saving data, your session has expired. Click "Logout" in the sidebar and log back in to refresh your JWT token.
                """
        ));

        articles.add(new KnowledgeArticle(
                "kb-site-navigation",
                "HireRanker Website Navigation & Directory",
                "Navigation",
                Arrays.asList("navigation", "menu", "pages", "routes", "where", "how to find", "links"),
                "Complete list of canonical routes and sections for Candidates and Recruiters.",
                """
                ### HireRanker Navigation Directory:
                **Candidate Portal**:
                - Dashboard: `/candidate-dashboard`
                - Find Jobs: `/find-jobs`
                - My Resume: `/my-resume`
                - My Applications: `/my-applications`
                - Live Interviews: `/interviews`
                - Profile & Settings: `/my-profile`

                **Recruiter/Admin SaaS Portal**:
                - Recruiter Command Center: `/dashboard`
                - Job Postings: `/job-postings`
                - Job Applicants: `/job-applicants`
                - Resume Screening: `/resume-screening`
                - Evaluation Criteria: `/evaluation-criteria`
                - Candidate Ranking: `/candidate-ranking`
                - Shortlisted Candidates: `/shortlisted-candidates`
                - Messages: `/messages`
                - Interview Scheduler: `/interview-scheduler`
                - Reports & Analytics: `/reports`
                - Settings: `/settings`
                """
        ));
    }

    /**
     * Retrieves the most relevant knowledge base articles for a user query.
     */
    public List<KnowledgeArticle> findRelevantArticles(String query, int topN) {
        if (query == null || query.isBlank()) {
            return articles.stream().limit(topN).collect(Collectors.toList());
        }

        String lowerQuery = query.toLowerCase().trim();
        List<String> queryWords = Arrays.stream(lowerQuery.split("\\s+"))
                .filter(w -> w.length() > 2)
                .toList();

        return articles.stream()
                .map(article -> new ScoredArticle(article, calculateScore(article, lowerQuery, queryWords)))
                .sorted(Comparator.comparingInt(ScoredArticle::score).reversed())
                .filter(sa -> sa.score() > 0)
                .limit(topN)
                .map(ScoredArticle::article)
                .collect(Collectors.toList());
    }

    private int calculateScore(KnowledgeArticle article, String lowerQuery, List<String> queryWords) {
        int score = 0;

        // Exact query substring in title or content
        if (article.title().toLowerCase().contains(lowerQuery)) {
            score += 50;
        }
        if (article.category().toLowerCase().contains(lowerQuery)) {
            score += 30;
        }

        // Keywords match
        for (String kw : article.keywords()) {
            if (lowerQuery.contains(kw.toLowerCase())) {
                score += 25;
            }
        }

        // Word overlap
        for (String word : queryWords) {
            if (article.title().toLowerCase().contains(word)) {
                score += 15;
            }
            if (article.fullContent().toLowerCase().contains(word)) {
                score += 5;
            }
        }

        return score;
    }

    public List<KnowledgeArticle> getAllArticles() {
        return new ArrayList<>(articles);
    }

    private record ScoredArticle(KnowledgeArticle article, int score) {}
}
