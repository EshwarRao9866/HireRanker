package spring.eshwar.dto.interview;

import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.Resume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Structured candidate profile representing pre-analyzed resume data.
 * Sent to interview AI instead of re-transmitting the entire unparsed PDF text on every question.
 */
public record StructuredCandidateProfileDto(
        Long candidateId,
        String candidateName,
        String currentTitle,
        String yearsOfExperience,
        List<String> coreSkills,
        String education,
        String projectsSummary,
        List<String> strengths
) {

    /**
     * Calculates the dynamic target question count (strictly between MIN=6 and MAX=10)
     * based on the depth of relevant skills required by the job and present in candidate profile.
     * Candidates with limited skills (1-3 skills) -> 6-7 questions.
     * Candidates with moderate skills (4-5 skills) -> 7-8 questions.
     * Candidates with broad skills (6-8 skills) -> 8-9 questions.
     * Candidates with many relevant skills (9+ skills) -> 10 questions.
     */
    public int calculateDynamicTargetQuestions(Job job) {
        int relevantSkillCount = getPrioritizedSkillList(job).size();
        int count;
        if (relevantSkillCount <= 2) {
            count = 6;
        } else if (relevantSkillCount <= 4) {
            count = 7;
        } else if (relevantSkillCount <= 6) {
            count = 8;
        } else if (relevantSkillCount <= 8) {
            count = 9;
        } else {
            count = 10;
        }
        return Math.max(6, Math.min(10, count));
    }

    /**
     * Collects and prioritizes relevant skills in order:
     * 1. Job required skills
     * 2. Candidate resume skills
     * 3. Core engineering disciplines (only if both job & candidate skills are minimal)
     */
    public List<String> getPrioritizedSkillList(Job job) {
        List<String> prioritized = new ArrayList<>();
        spring.eshwar.service.skill.SkillNormalizationService normalizer = new spring.eshwar.service.skill.SkillNormalizationService();

        // 1. Job Required Skills first
        if (job != null && job.getRequiredSkills() != null && !job.getRequiredSkills().isBlank()) {
            for (String s : normalizer.decomposeAndNormalizeSkills(job.getRequiredSkills())) {
                if (!prioritized.contains(s)) {
                    prioritized.add(s);
                }
            }
        }

        // 2. Candidate Core Resume Skills second
        if (coreSkills != null) {
            for (String s : coreSkills) {
                if (!prioritized.contains(s)) {
                    prioritized.add(s);
                }
            }
        }

        // 3. Fallback core engineering disciplines only if total list is sparse (< 3)
        if (prioritized.size() < 3) {
            List<String> coreDisciplines = List.of(
                    "Software Engineering Fundamentals",
                    "REST APIs & Integration",
                    "Database & SQL",
                    "System Architecture",
                    "Practical Debugging"
            );
            for (String d : coreDisciplines) {
                if (!prioritized.contains(d)) {
                    prioritized.add(d);
                }
            }
        }

        return prioritized;
    }

    public static StructuredCandidateProfileDto fromCandidateAndResume(Candidate candidate, Resume resume) {
        String name = (candidate != null && candidate.getFullName() != null) ? candidate.getFullName() : "Candidate";
        String exp = (candidate != null && candidate.getExperience() != null) ? candidate.getExperience() : "3+ years";
        String edu = (candidate != null && candidate.getEducation() != null) ? candidate.getEducation() : "Computer Science / Engineering";

        List<String> skills = new ArrayList<>();
        List<String> detectedStrengths = new ArrayList<>();
        String projects = "Enterprise Application Architecture, REST API Design, Microservices";

        // 1. Check AI Resume Analysis JSON first (most accurate and granular)
        if (resume != null && resume.getAiAnalysisJson() != null && !resume.getAiAnalysisJson().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(resume.getAiAnalysisJson());
                if (root.has("skills") && root.get("skills").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode sNode : root.get("skills")) {
                        String s = sNode.asText("").trim();
                        if (!s.isEmpty() && !skills.contains(s)) {
                            skills.add(s);
                        }
                    }
                }
                if (root.has("strengths") && root.get("strengths").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode stNode : root.get("strengths")) {
                        String st = stNode.asText("").trim();
                        if (!st.isEmpty() && !detectedStrengths.contains(st)) {
                            detectedStrengths.add(st);
                        }
                    }
                }
                if (root.has("experienceSummary") && !root.path("experienceSummary").asText("").isBlank()) {
                    projects = root.path("experienceSummary").asText("").trim();
                }
            } catch (Exception ignored) {}
        }

        // 2. Supplement with candidate profile skills
        spring.eshwar.service.skill.SkillNormalizationService skillNormalizer = new spring.eshwar.service.skill.SkillNormalizationService();
        if (candidate != null && candidate.getSkills() != null && !candidate.getSkills().isBlank()) {
            for (String norm : skillNormalizer.decomposeAndNormalizeSkills(candidate.getSkills())) {
                if (!skills.contains(norm)) {
                    skills.add(norm);
                }
            }
        }

        // 3. Fallback or enrichment: scan full resume extracted text
        if (resume != null && resume.getExtractedText() != null && !resume.getExtractedText().isBlank()) {
            List<String> resumeSkills = skillNormalizer.extractSkillsFromResumeText(resume.getExtractedText());
            for (String s : resumeSkills) {
                if (!skills.contains(s)) {
                    skills.add(s);
                }
            }
            String textLower = resume.getExtractedText().toLowerCase();
            if (textLower.contains("project")) {
                int pIdx = textLower.indexOf("project");
                int endIdx = Math.min(textLower.length(), pIdx + 300);
                projects = resume.getExtractedText().substring(pIdx, endIdx).replaceAll("\\s+", " ").trim();
            }
        }

        // If no explicit skills found, do NOT invent fake specific technologies; keep list genuine
        String primarySkill = !skills.isEmpty() ? skills.get(0) : "Software";
        String dynamicTitle = primarySkill + " Engineer";

        if (detectedStrengths.isEmpty()) {
            detectedStrengths.add("General technical aptitude");
            detectedStrengths.add("Problem-solving orientation");
        }

        return new StructuredCandidateProfileDto(
                candidate != null ? candidate.getId() : null,
                name,
                dynamicTitle,
                exp,
                skills,
                edu,
                projects,
                detectedStrengths
        );
    }
}
