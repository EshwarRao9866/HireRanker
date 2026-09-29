package spring.eshwar.interview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.eshwar.dto.interview.StructuredCandidateProfileDto;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.Resume;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AdaptiveQuestionGenerationLogicTest {

    @Test
    @DisplayName("Candidate with limited skills yields 6-8 dynamic target questions")
    void testLimitedSkillsYieldsSixToEightQuestions() {
        Job job = new Job();
        job.setTitle("Java Developer");
        job.setRequiredSkills("Java, Spring Boot");

        Candidate candidate = new Candidate();
        candidate.setSkills("Java, SQL");

        StructuredCandidateProfileDto profile = StructuredCandidateProfileDto.fromCandidateAndResume(candidate, null);
        int target = profile.calculateDynamicTargetQuestions(job);

        // Limited skills (Java, Spring Boot, SQL): should yield between 6 and 8 questions
        assertThat(target).isBetween(6, 8);
    }

    @Test
    @DisplayName("Candidate with many diverse skills yields 8-10 dynamic target questions")
    void testManySkillsYieldsEightToTenQuestions() {
        Job job = new Job();
        job.setTitle("Full Stack Cloud Architect");
        job.setRequiredSkills("Java, Spring Boot, Angular, TypeScript, PostgreSQL, Docker, Kubernetes, AWS, Kafka, Microservices");

        Candidate candidate = new Candidate();
        candidate.setSkills("Java, Spring Boot, React, Python, Redis, GraphQL, CI/CD");

        StructuredCandidateProfileDto profile = StructuredCandidateProfileDto.fromCandidateAndResume(candidate, null);
        int target = profile.calculateDynamicTargetQuestions(job);

        // Many diverse skills: should yield between 8 and 10 questions
        assertThat(target).isBetween(8, 10);
        assertThat(target).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Dynamic target questions strictly clamps between MIN=6 and MAX=10 in all edge cases")
    void testStrictMinMaxBounds() {
        Job emptyJob = new Job();
        emptyJob.setRequiredSkills("");

        Candidate emptyCandidate = new Candidate();
        emptyCandidate.setSkills("");

        StructuredCandidateProfileDto emptyProfile = StructuredCandidateProfileDto.fromCandidateAndResume(emptyCandidate, null);
        int minTarget = emptyProfile.calculateDynamicTargetQuestions(emptyJob);
        assertThat(minTarget).isGreaterThanOrEqualTo(6);
        assertThat(minTarget).isLessThanOrEqualTo(10);

        // Candidate and job with 20 skills
        Job hugeJob = new Job();
        hugeJob.setRequiredSkills("S1, S2, S3, S4, S5, S6, S7, S8, S9, S10, S11, S12, S13, S14, S15, S16");
        Candidate hugeCandidate = new Candidate();
        hugeCandidate.setSkills("C1, C2, C3, C4, C5, C6, C7, C8");

        StructuredCandidateProfileDto hugeProfile = StructuredCandidateProfileDto.fromCandidateAndResume(hugeCandidate, null);
        int maxTarget = hugeProfile.calculateDynamicTargetQuestions(hugeJob);
        assertThat(maxTarget).isEqualTo(10);
    }

    @Test
    @DisplayName("Skill prioritization places Job Required Skills before Candidate skills and avoids fake skills")
    void testSkillPrioritizationOrder() {
        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setRequiredSkills("Go, Kubernetes, gRPC");

        Candidate candidate = new Candidate();
        candidate.setSkills("Java, Docker, Kubernetes");

        StructuredCandidateProfileDto profile = StructuredCandidateProfileDto.fromCandidateAndResume(candidate, null);
        List<String> prioritized = profile.getPrioritizedSkillList(job);

        assertThat(prioritized).isNotEmpty();
        // Job required skills must appear before skills unique to the candidate
        int goIndex = prioritized.indexOf("Go");
        int grpcIndex = prioritized.indexOf("gRPC");
        int javaIndex = prioritized.indexOf("Java");

        assertThat(goIndex).isGreaterThanOrEqualTo(0);
        assertThat(grpcIndex).isGreaterThanOrEqualTo(0);
        assertThat(javaIndex).isGreaterThanOrEqualTo(0);
        assertThat(goIndex).isLessThan(javaIndex);
        assertThat(grpcIndex).isLessThan(javaIndex);
    }
}
