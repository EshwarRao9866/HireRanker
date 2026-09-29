package spring.eshwar.service.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Centralized Skill Normalization, Extraction, and Matching Service.
 * Ensures consistent canonical skill representation across resume parsing,
 * job description evaluation, ATS scoring, and interview question generation.
 */
@Service
public class SkillNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(SkillNormalizationService.class);

    /**
     * Canonical mapping from lowercase variations/aliases to standard canonical skill names.
     */
    private static final Map<String, String> CANONICAL_MAP = new LinkedHashMap<>();

    /**
     * Regex patterns for boundary-safe extraction from full resume or job description text.
     */
    private static final List<SkillExtractionPattern> EXTRACTION_PATTERNS = new ArrayList<>();

    record SkillExtractionPattern(String canonicalSkill, Pattern pattern) {}

    static {
        // --- Web Fundamentals ---
        registerCanonical("HTML", "html", "html5", "xhtml");
        registerCanonical("CSS", "css", "css3");
        registerCanonical("JavaScript", "javascript", "js", "ecmascript", "es6", "es2015", "vanilla js");
        registerCanonical("TypeScript", "typescript", "ts");

        // --- APIs & Web Services ---
        registerCanonical("REST API", "rest api", "rest apis", "restful api", "restful apis", "rest", "restful", "rest web services", "restful web services");
        registerCanonical("GraphQL", "graphql");
        registerCanonical("gRPC", "grpc");
        registerCanonical("SOAP", "soap", "soap api", "soap web services");
        registerCanonical("WebSocket", "websocket", "websockets");

        // --- Styling & UI Frameworks ---
        registerCanonical("Bootstrap", "bootstrap", "bootstrap 3", "bootstrap 4", "bootstrap 5", "bootstrap3", "bootstrap4", "bootstrap5");
        registerCanonical("Tailwind CSS", "tailwind", "tailwindcss", "tailwind css");
        registerCanonical("Sass", "sass", "scss");
        registerCanonical("Material UI", "material ui", "mui", "material design");

        // --- Frontend Frameworks & Libraries ---
        // CRITICAL: Angular != AngularJS, React != React Native
        registerCanonical("Angular", "angular", "angular 2+", "angular 14", "angular 15", "angular 16", "angular 17", "angular 18");
        registerCanonical("AngularJS", "angularjs", "angular.js", "angular 1.x");
        registerCanonical("React", "react", "reactjs", "react.js");
        registerCanonical("React Native", "react native", "react-native");
        registerCanonical("Vue.js", "vue", "vuejs", "vue.js", "vue 2", "vue 3");
        registerCanonical("Next.js", "nextjs", "next.js", "next");

        // --- Backend Frameworks & Runtimes ---
        registerCanonical("Spring Boot", "spring boot", "springboot", "spring-boot");
        registerCanonical("Spring", "spring", "spring framework", "spring mvc", "spring security", "spring data");
        registerCanonical("Node.js", "node", "nodejs", "node.js");
        registerCanonical("Express.js", "express", "expressjs", "express.js");
        registerCanonical("Django", "django");
        registerCanonical("Flask", "flask");
        registerCanonical("FastAPI", "fastapi", "fast api");
        registerCanonical("ASP.NET", "asp.net", "asp.net core", ".net core", ".net");

        // --- Programming Languages ---
        // CRITICAL: Java != JavaScript, Python != PyTorch
        registerCanonical("Java", "java", "java 8", "java 11", "java 17", "java 21", "core java");
        registerCanonical("Python", "python", "python 3", "python3");
        registerCanonical("C++", "c++", "cpp");
        registerCanonical("C#", "c#", "csharp");
        registerCanonical("C", "c language");
        registerCanonical("Go", "golang", "go");
        registerCanonical("Rust", "rust");
        registerCanonical("PHP", "php");
        registerCanonical("Ruby", "ruby");

        // --- Databases & Persistence ---
        registerCanonical("MySQL", "mysql");
        registerCanonical("PostgreSQL", "postgresql", "postgres", "psql");
        registerCanonical("SQL", "sql", "relational databases", "rdbms");
        registerCanonical("MongoDB", "mongodb", "mongo", "nosql");
        registerCanonical("SQLite", "sqlite");
        registerCanonical("Oracle DB", "oracle", "oracle db", "oracle database");
        registerCanonical("Redis", "redis");
        registerCanonical("Hibernate", "hibernate", "jpa", "spring data jpa");

        // --- Cloud & DevOps ---
        // CRITICAL: Git != GitHub by default
        registerCanonical("Git", "git", "git scm", "git version control");
        registerCanonical("GitHub", "github");
        registerCanonical("GitLab", "gitlab");
        registerCanonical("Docker", "docker", "containerization", "containers");
        registerCanonical("Kubernetes", "kubernetes", "k8s");
        registerCanonical("AWS", "aws", "amazon web services", "aws cloud");
        registerCanonical("Azure", "azure", "microsoft azure");
        registerCanonical("GCP", "gcp", "google cloud", "google cloud platform");
        registerCanonical("CI/CD", "ci/cd", "cicd", "continuous integration", "continuous deployment");
        registerCanonical("Jenkins", "jenkins");
        registerCanonical("Linux", "linux", "unix", "ubuntu", "bash", "shell scripting");

        // --- Architecture & Messaging ---
        registerCanonical("Microservices", "microservices", "microservice", "microservice architecture");
        registerCanonical("Kafka", "kafka", "apache kafka");
        registerCanonical("RabbitMQ", "rabbitmq");
        registerCanonical("Maven", "maven");
        registerCanonical("Gradle", "gradle");

        // --- AI & Data Science ---
        registerCanonical("PyTorch", "pytorch");
        registerCanonical("TensorFlow", "tensorflow");
        registerCanonical("Machine Learning", "machine learning", "ml");

        // --- Build boundary-safe extraction patterns for full resume text ---
        initExtractionPatterns();
    }

    private static void registerCanonical(String canonical, String... variations) {
        CANONICAL_MAP.put(canonical.toLowerCase(Locale.ROOT), canonical);
        for (String v : variations) {
            CANONICAL_MAP.put(v.toLowerCase(Locale.ROOT), canonical);
        }
    }

    private static void initExtractionPatterns() {
        // High priority specific multi-word patterns first
        addPattern("REST API", "(?i)\\b(?:rest(?:ful)?\\s+(?:apis?|web\\s+services?)|restful\\b)");
        addPattern("Spring Boot", "(?i)\\bspring\\s*boot\\b");
        addPattern("Spring", "(?i)\\bspring\\s+(?:framework|mvc|security|data)\\b");
        addPattern("AngularJS", "(?i)\\bangular\\s*js\\b");
        addPattern("Angular", "(?i)\\bangular\\b(?!\\s*js)");
        addPattern("React Native", "(?i)\\breact[\\s-]native\\b");
        addPattern("React", "(?i)\\breact(?:js|\\.js)?\\b(?!\\s*native)");
        addPattern("Node.js", "(?i)\\bnode(?:js|\\.js)?\\b");
        addPattern("Next.js", "(?i)\\bnext(?:js|\\.js)?\\b");
        addPattern("Vue.js", "(?i)\\bvue(?:js|\\.js)?\\b");
        addPattern("Bootstrap", "(?i)\\bbootstrap(?:\\s*[345])?\\b");
        addPattern("Tailwind CSS", "(?i)\\btailwind(?:css|\\s+css)?\\b");

        // Programming Languages - carefully bounded
        addPattern("JavaScript", "(?i)(?:\\bjavascript\\b|\\bjs\\b|\\becmascript\\b)");
        addPattern("TypeScript", "(?i)(?:\\btypescript\\b|\\bts\\b)");
        addPattern("HTML", "(?i)\\bhtml(?:5)?\\b");
        addPattern("CSS", "(?i)\\bcss(?:3)?\\b");
        addPattern("Java", "(?i)\\bjava\\b(?!\\s*script)");
        addPattern("Python", "(?i)\\bpython(?:3)?\\b(?!\\s*torch)");
        addPattern("C++", "(?i)(?:\\bc\\+\\+\\b|\\bcpp\\b)");
        addPattern("C#", "(?i)(?:\\bc#\\b|\\bcsharp\\b)");

        // Databases
        addPattern("MySQL", "(?i)\\bmysql\\b");
        addPattern("PostgreSQL", "(?i)\\b(?:postgresql|postgres|psql)\\b");
        addPattern("SQL", "(?i)\\bsql\\b(?!ite)");
        addPattern("MongoDB", "(?i)\\b(?:mongodb|mongo)\\b");
        addPattern("Redis", "(?i)\\bredis\\b");

        // Version Control: Git vs GitHub
        // Matches Git when it appears as standalone Git, Git version control, Git/GitHub, Git and GitHub, etc.
        addPattern("Git", "(?i)(?:\\bgit\\b(?!\\s*hub)|\\bgit\\s*(?:/|&|and)\\s*github\\b|\\bgit\\s+version\\s+control\\b|\\bsource\\s+control\\b)");
        addPattern("GitHub", "(?i)\\bgithub\\b");

        // DevOps & Cloud
        addPattern("Docker", "(?i)\\bdocker\\b");
        addPattern("Kubernetes", "(?i)(?:\\bkubernetes\\b|\\bk8s\\b)");
        addPattern("AWS", "(?i)(?:\\baws\\b|\\bamazon\\s+web\\s+services\\b)");
        addPattern("Azure", "(?i)(?:\\bazure\\b|\\bmicrosoft\\s+azure\\b)");
        addPattern("CI/CD", "(?i)(?:\\bci/cd\\b|\\bcicd\\b)");
        addPattern("Jenkins", "(?i)\\bjenkins\\b");
        addPattern("Linux", "(?i)\\blinux\\b");
        addPattern("Microservices", "(?i)\\bmicroservices?\\b");
        addPattern("Kafka", "(?i)(?:\\bapache\\s+)?\\bkafka\\b");
        addPattern("Hibernate", "(?i)\\b(?:hibernate|jpa)\\b");
        addPattern("Maven", "(?i)\\bmaven\\b");
        addPattern("GraphQL", "(?i)\\bgraphql\\b");
        addPattern("PyTorch", "(?i)\\bpytorch\\b");
        addPattern("TensorFlow", "(?i)\\btensorflow\\b");
    }

    private static void addPattern(String canonical, String regex) {
        EXTRACTION_PATTERNS.add(new SkillExtractionPattern(canonical, Pattern.compile(regex)));
    }

    /**
     * Normalizes a single raw skill string into its canonical representation.
     * Returns the canonical name if recognized, or trimmed title-case if unrecognized.
     */
    public String normalizeSkill(String rawSkill) {
        if (rawSkill == null || rawSkill.isBlank()) {
            return "";
        }
        String cleaned = rawSkill.trim();
        String lower = cleaned.toLowerCase(Locale.ROOT);

        // 1. Direct dictionary match
        if (CANONICAL_MAP.containsKey(lower)) {
            return CANONICAL_MAP.get(lower);
        }

        // 2. Strip version suffix (e.g. "Java 17" -> "Java", "Angular 16" -> "Angular", "Python 3.11" -> "Python")
        String stripped = stripVersion(cleaned);
        if (!stripped.equalsIgnoreCase(cleaned) && CANONICAL_MAP.containsKey(stripped.toLowerCase(Locale.ROOT))) {
            return CANONICAL_MAP.get(stripped.toLowerCase(Locale.ROOT));
        }

        // 3. Fallback: preserve original with clean capitalization
        return cleaned;
    }

    /**
     * Decomposes compound skills (e.g. "HTML5/CSS3", "JavaScript / TypeScript", "HTML + CSS")
     * and normalizes all constituent skills into canonical representations.
     */
    public List<String> decomposeAndNormalizeSkills(String rawTextOrSkills) {
        if (rawTextOrSkills == null || rawTextOrSkills.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> resultSet = new LinkedHashSet<>();

        // First, split by comma or newline or semicolon to separate multiple items
        String[] items = rawTextOrSkills.split("[,;\\n]+");
        for (String item : items) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) continue;

            // Check if the item as a whole directly matches a known canonical (e.g. "CI/CD", "RESTful APIs", "Spring Boot")
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (CANONICAL_MAP.containsKey(lower)) {
                resultSet.add(CANONICAL_MAP.get(lower));
                continue;
            }

            // Check for compound delimiters: "/", "\", "&", "+", or " and "
            // BUT protect known unified terms like "CI/CD" which were handled above
            if (isCompoundSkill(trimmed)) {
                List<String> subTokens = splitCompoundTokens(trimmed);
                for (String sub : subTokens) {
                    String norm = normalizeSkill(sub);
                    if (!norm.isBlank()) {
                        resultSet.add(norm);
                    }
                }
            } else {
                String norm = normalizeSkill(trimmed);
                if (!norm.isBlank()) {
                    resultSet.add(norm);
                }
            }
        }

        return new ArrayList<>(resultSet);
    }

    private boolean isCompoundSkill(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.equals("ci/cd") || lower.equals("c/c++") || lower.equals("tcp/ip") || lower.equals("pl/sql")) {
            return false;
        }
        return s.contains("/") || s.contains("\\") || s.contains("&") || s.contains("+") || s.matches("(?i).*\\b(?:and)\\b.*");
    }

    private List<String> splitCompoundTokens(String compound) {
        // Split by /, \, &, +, or " and "
        String[] parts = compound.split("(?:/|\\\\|&|\\+|\\s+(?i:and)\\s+)");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    private String stripVersion(String s) {
        // Removes trailing version numbers like "17", "v5", "3.0", "3.11"
        return s.replaceAll("(?i)\\s+(?:v\\.?\\s*)?\\d+(?:\\.\\d+)*$", "")
                .replaceAll("(?i)(?<=[a-zA-Z])\\s*\\d+(?:\\.\\d+)*$", "")
                .trim();
    }

    /**
     * Scans the COMPLETE extracted resume text (including Projects, Experience, Education, Certifications)
     * using boundary-safe extraction patterns to find all mentioned skills.
     */
    public List<String> extractSkillsFromResumeText(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> detected = new LinkedHashSet<>();

        for (SkillExtractionPattern sep : EXTRACTION_PATTERNS) {
            Matcher m = sep.pattern().matcher(resumeText);
            if (m.find()) {
                detected.add(sep.canonicalSkill());
            }
        }

        // Special handling for Git vs GitHub:
        // Ensure GitHub alone without Git context does NOT extract Git
        if (detected.contains("Git")) {
            if (!hasGitEvidence(resumeText)) {
                detected.remove("Git");
            }
        }

        return new ArrayList<>(detected);
    }

    /**
     * Determines whether the resume contains genuine evidence of Git usage.
     */
    public boolean hasGitEvidence(String text) {
        if (text == null || text.isBlank()) return false;
        // Check for Git as standalone word (not github.com or standalone github),
        // or explicit version control context
        Pattern gitPattern = Pattern.compile("(?i)(?:\\bgit\\b(?!\\s*hub)|\\bgit\\s*(?:/|&|and)\\s*github\\b|\\bgit\\s+version\\s+control\\b|\\bsource\\s+control\\b|\\bgit\\s+repository\\b|\\bgithub\\s+repository\\b|\\bgit\\s+workflow\\b)");
        return gitPattern.matcher(text).find();
    }

    /**
     * Extracts and normalizes required skills from job description text and required skills field.
     */
    public List<String> extractSkillsFromJob(String requiredSkillsStr, String jobDescription) {
        Set<String> skills = new LinkedHashSet<>();

        if (requiredSkillsStr != null && !requiredSkillsStr.isBlank()) {
            skills.addAll(decomposeAndNormalizeSkills(requiredSkillsStr));
        }

        if (jobDescription != null && !jobDescription.isBlank()) {
            // Also scan job description for any additional key technologies mentioned
            for (SkillExtractionPattern sep : EXTRACTION_PATTERNS) {
                Matcher m = sep.pattern().matcher(jobDescription);
                if (m.find()) {
                    skills.add(sep.canonicalSkill());
                }
            }
        }

        return new ArrayList<>(skills);
    }

    /**
     * Core matching logic with 5-tier evaluation:
     * 1. Exact normalized match
     * 2. Alias / Abbreviation match
     * 3. Version-aware match
     * 4. Compound-skill match
     * 5. Contextual evidence search in resume text
     */
    public SkillMatchResult matchSkills(Collection<String> resumeSkills,
                                        Collection<String> requiredJobSkills,
                                        String fullResumeText) {
        Set<String> normalizedResumeSkills = new LinkedHashSet<>();
        if (resumeSkills != null) {
            for (String s : resumeSkills) {
                normalizedResumeSkills.addAll(decomposeAndNormalizeSkills(s));
            }
        }

        // Also enrich with skills detected directly in full text
        if (fullResumeText != null && !fullResumeText.isBlank()) {
            normalizedResumeSkills.addAll(extractSkillsFromResumeText(fullResumeText));
        }

        Set<String> normalizedJobSkills = new LinkedHashSet<>();
        if (requiredJobSkills != null) {
            for (String s : requiredJobSkills) {
                normalizedJobSkills.addAll(decomposeAndNormalizeSkills(s));
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String jobSkill : normalizedJobSkills) {
            boolean isMatched = false;

            // Tier 1: Exact normalized match
            for (String candSkill : normalizedResumeSkills) {
                if (jobSkill.equalsIgnoreCase(candSkill)) {
                    matched.add(jobSkill);
                    isMatched = true;
                    break;
                }
            }
            if (isMatched) continue;

            // Tier 2: Alias / Abbreviation match
            String canonicalJob = normalizeSkill(jobSkill);
            for (String candSkill : normalizedResumeSkills) {
                String canonicalCand = normalizeSkill(candSkill);
                if (canonicalJob.equalsIgnoreCase(canonicalCand)) {
                    matched.add(canonicalJob);
                    isMatched = true;
                    break;
                }
            }
            if (isMatched) continue;

            // Tier 3: Version-aware match (e.g. Bootstrap 5 vs Bootstrap, HTML5 vs HTML, Java 17 vs Java)
            String baseJob = stripVersion(jobSkill);
            for (String candSkill : normalizedResumeSkills) {
                String baseCand = stripVersion(candSkill);
                if (baseJob.equalsIgnoreCase(baseCand) || normalizeSkill(baseJob).equalsIgnoreCase(normalizeSkill(baseCand))) {
                    matched.add(canonicalJob.isBlank() ? baseJob : canonicalJob);
                    isMatched = true;
                    break;
                }
            }
            if (isMatched) continue;

            // Tier 4: Compound-skill match
            // If jobSkill was a compound (e.g. "HTML/CSS") check if all parts are present
            if (isCompoundSkill(jobSkill)) {
                List<String> parts = splitCompoundTokens(jobSkill);
                boolean allPartsPresent = true;
                for (String part : parts) {
                    String normPart = normalizeSkill(part);
                    if (!normalizedResumeSkills.stream().anyMatch(rs -> rs.equalsIgnoreCase(normPart) || normalizeSkill(rs).equalsIgnoreCase(normPart))) {
                        allPartsPresent = false;
                        break;
                    }
                }
                if (allPartsPresent && !parts.isEmpty()) {
                    matched.add(jobSkill);
                    isMatched = true;
                    continue;
                }
            }

            // Tier 5: Contextual evidence search in complete resume text
            if (fullResumeText != null && !fullResumeText.isBlank()) {
                if (hasEvidenceInText(jobSkill, fullResumeText)) {
                    matched.add(jobSkill);
                    isMatched = true;
                    continue;
                }
            }

            // Only mark missing if all 5 tiers failed
            missing.add(jobSkill);
        }

        // Deduplicate matched
        List<String> distinctMatched = matched.stream().distinct().collect(Collectors.toList());
        List<String> distinctMissing = missing.stream().distinct().collect(Collectors.toList());

        // Recommended skills: ONLY genuine missing skills, NEVER recommending something already present
        List<String> recommended = new ArrayList<>();
        for (String m : distinctMissing) {
            if (!distinctMatched.contains(m) && !normalizedResumeSkills.contains(m)) {
                recommended.add(m);
            }
        }

        // Calculate accurate match ratio
        double ratio = normalizedJobSkills.isEmpty() ? 1.0 : ((double) distinctMatched.size() / normalizedJobSkills.size());

        return new SkillMatchResult(distinctMatched, distinctMissing, recommended, ratio, new ArrayList<>(normalizedResumeSkills), new ArrayList<>(normalizedJobSkills));
    }

    /**
     * Checks if a skill has concrete evidence in the raw text using its extraction pattern.
     */
    public boolean hasEvidenceInText(String skill, String text) {
        if (text == null || text.isBlank() || skill == null || skill.isBlank()) return false;

        String canonical = normalizeSkill(skill);

        // Special case for Git
        if ("Git".equalsIgnoreCase(canonical)) {
            return hasGitEvidence(text);
        }

        for (SkillExtractionPattern sep : EXTRACTION_PATTERNS) {
            if (sep.canonicalSkill().equalsIgnoreCase(canonical)) {
                Matcher m = sep.pattern().matcher(text);
                if (m.find()) return true;
            }
        }

        // Word-boundary fallback
        String escaped = Pattern.quote(skill);
        Pattern p = Pattern.compile("(?i)\\b" + escaped + "\\b");
        return p.matcher(text).find();
    }

    public record SkillMatchResult(
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> recommendedSkills,
            double matchRatio,
            List<String> normalizedResumeSkills,
            List<String> normalizedJobSkills
    ) {}
}
