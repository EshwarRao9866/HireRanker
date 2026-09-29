package spring.eshwar.service.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.eshwar.service.skill.SkillNormalizationService.SkillMatchResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillNormalizationServiceTest {

    private SkillNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new SkillNormalizationService();
    }

    @Test
    @DisplayName("TEST 1: Resume HTML5 vs Job HTML -> MATCH")
    void test1_html5_vs_html() {
        SkillMatchResult result = service.matchSkills(List.of("HTML5"), List.of("HTML"), "");
        assertTrue(result.matchedSkills().contains("HTML"), "HTML should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 2: Resume CSS3 vs Job CSS -> MATCH")
    void test2_css3_vs_css() {
        SkillMatchResult result = service.matchSkills(List.of("CSS3"), List.of("CSS"), "");
        assertTrue(result.matchedSkills().contains("CSS"), "CSS should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 3: Resume JS vs Job JavaScript -> MATCH")
    void test3_js_vs_javascript() {
        SkillMatchResult result = service.matchSkills(List.of("JS"), List.of("JavaScript"), "");
        assertTrue(result.matchedSkills().contains("JavaScript"), "JavaScript should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 4: Resume TS vs Job TypeScript -> MATCH")
    void test4_ts_vs_typescript() {
        SkillMatchResult result = service.matchSkills(List.of("TS"), List.of("TypeScript"), "");
        assertTrue(result.matchedSkills().contains("TypeScript"), "TypeScript should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 5: Resume RESTful APIs vs Job REST API -> MATCH")
    void test5_restfulApis_vs_restApi() {
        SkillMatchResult result = service.matchSkills(List.of("RESTful APIs"), List.of("REST API"), "");
        assertTrue(result.matchedSkills().contains("REST API"), "REST API should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 6: Resume Bootstrap 5 vs Job Bootstrap -> MATCH")
    void test6_bootstrap5_vs_bootstrap() {
        SkillMatchResult result = service.matchSkills(List.of("Bootstrap 5"), List.of("Bootstrap"), "");
        assertTrue(result.matchedSkills().contains("Bootstrap"), "Bootstrap should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 7: Resume HTML/CSS vs Job HTML + CSS -> MATCH")
    void test7_compoundHtmlCss_vs_jobHtmlPlusCss() {
        List<String> resumeSkills = service.decomposeAndNormalizeSkills("HTML/CSS");
        List<String> jobSkills = service.decomposeAndNormalizeSkills("HTML + CSS");

        SkillMatchResult result = service.matchSkills(resumeSkills, jobSkills, "");
        assertTrue(result.matchedSkills().contains("HTML"), "HTML should be matched");
        assertTrue(result.matchedSkills().contains("CSS"), "CSS should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 8: Resume Git vs Job Git -> MATCH")
    void test8_git_vs_git() {
        SkillMatchResult result = service.matchSkills(List.of("Git"), List.of("Git"), "");
        assertTrue(result.matchedSkills().contains("Git"), "Git should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 9: Skills in project description must be detected")
    void test9_project_description_detection() {
        String projectText = "PROJECT: Developed an Angular application using Spring Boot, REST APIs, MySQL and Git.";
        List<String> detected = service.extractSkillsFromResumeText(projectText);

        assertTrue(detected.contains("Angular"), "Angular must be detected");
        assertTrue(detected.contains("Spring Boot"), "Spring Boot must be detected");
        assertTrue(detected.contains("REST API"), "REST API must be detected");
        assertTrue(detected.contains("MySQL"), "MySQL must be detected");
        assertTrue(detected.contains("Git"), "Git must be detected");
    }

    @Test
    @DisplayName("TEST 10: Resume Java + Spring Boot vs Job React + Node.js -> genuinely missing")
    void test10_genuinely_missing_skills() {
        List<String> resumeSkills = List.of("Java", "Spring Boot");
        List<String> jobSkills = List.of("React", "Node.js");

        SkillMatchResult result = service.matchSkills(resumeSkills, jobSkills, "Worked on Java and Spring Boot microservices.");
        assertTrue(result.matchedSkills().isEmpty(), "No skills should match");
        assertTrue(result.missingSkills().contains("React"), "React is genuinely missing");
        assertTrue(result.missingSkills().contains("Node.js"), "Node.js is genuinely missing");
    }

    @Test
    @DisplayName("TEST 11: Resume GitHub vs Job Git -> Do NOT automatically match without version control context")
    void test11_github_alone_does_not_match_git() {
        String resumeText = "Portfolio: https://github.com/johndoe";
        List<String> detected = service.extractSkillsFromResumeText(resumeText);
        assertFalse(detected.contains("Git"), "Git should NOT be detected from github.com profile link alone");

        SkillMatchResult result = service.matchSkills(List.of("GitHub"), List.of("Git"), resumeText);
        assertFalse(result.matchedSkills().contains("Git"), "Git should not be matched from GitHub profile link");
        assertTrue(result.missingSkills().contains("Git"), "Git should be missing");
    }

    @Test
    @DisplayName("TEST 12: Resume Git and GitHub vs Job Git -> MATCH")
    void test12_git_and_github_matches_git() {
        String resumeText = "Version control: Git and GitHub for team collaboration.";
        List<String> detected = service.extractSkillsFromResumeText(resumeText);
        assertTrue(detected.contains("Git"), "Git must be detected when Git and GitHub is used");

        SkillMatchResult result = service.matchSkills(List.of("Git", "GitHub"), List.of("Git"), resumeText);
        assertTrue(result.matchedSkills().contains("Git"), "Git should be matched");
        assertTrue(result.missingSkills().isEmpty(), "No missing skills expected");
    }

    @Test
    @DisplayName("TEST 13: Full normalization & matching with 0 false missing and no duplicate recommendations")
    void test13_full_stack_matching_no_false_missing() {
        String resumeSkillsStr = "HTML5, CSS3, Bootstrap 5, JS, RESTful APIs, Git";
        String jobSkillsStr = "HTML, CSS, Bootstrap, JavaScript, REST API, Git";

        List<String> resumeSkills = service.decomposeAndNormalizeSkills(resumeSkillsStr);
        List<String> jobSkills = service.decomposeAndNormalizeSkills(jobSkillsStr);

        SkillMatchResult result = service.matchSkills(resumeSkills, jobSkills, "");

        assertEquals(6, result.matchedSkills().size(), "All 6 skills must be matched");
        assertTrue(result.matchedSkills().contains("HTML"));
        assertTrue(result.matchedSkills().contains("CSS"));
        assertTrue(result.matchedSkills().contains("Bootstrap"));
        assertTrue(result.matchedSkills().contains("JavaScript"));
        assertTrue(result.matchedSkills().contains("REST API"));
        assertTrue(result.matchedSkills().contains("Git"));

        assertTrue(result.missingSkills().isEmpty(), "Missing skills must be completely empty");

        // Recommendations must NOT recommend any of the matched skills
        for (String rec : result.recommendedSkills()) {
            assertFalse(result.matchedSkills().contains(rec), "Recommended skill must not be an already matched skill: " + rec);
        }
    }
}
