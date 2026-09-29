package spring.eshwar.interview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.eshwar.entity.InterviewQuestion;
import spring.eshwar.entity.Job;
import spring.eshwar.service.AiInterviewService.EvaluatedAnswerDto;
import spring.eshwar.service.ai.InterviewAIService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AIAnswerEvaluationTest {

    @Autowired
    private InterviewAIService interviewAiService;

    private InterviewQuestion createSampleQuestion() {
        InterviewQuestion q = new InterviewQuestion();
        q.setQuestionText("Explain how Java garbage collection works, including heap generations, minor GC, and mark-sweep algorithms.");
        q.setCategory("Language Fundamentals & Core Mechanisms");
        q.setDifficulty("Intermediate");
        q.setExpectedConcepts("heap generations, young generation, old generation, minor GC, major GC, mark-sweep, memory allocation");
        q.setKeyPoints("Explains Eden, Survivor spaces, tenure threshold, and mark-sweep-compact phase accurately");
        return q;
    }

    @Test
    @DisplayName("Empty or null answer yields zero scores and INCORRECT classification (No fake scores)")
    void testEmptyAnswerYieldsZero() {
        InterviewQuestion q = createSampleQuestion();
        Job job = new Job();
        job.setTitle("Java Backend Developer");

        EvaluatedAnswerDto evalEmpty = interviewAiService.evaluateAnswer(q, "", 0.0, 0.0, job);
        assertThat(evalEmpty.technicalScore()).isEqualTo(0.0);
        assertThat(evalEmpty.overallScore()).isEqualTo(0.0);
        assertThat(evalEmpty.correct()).isFalse();
        assertThat(evalEmpty.correctnessClassification()).isEqualTo("INCORRECT");

        EvaluatedAnswerDto evalNull = interviewAiService.evaluateAnswer(q, null, 0.0, 0.0, job);
        assertThat(evalNull.technicalScore()).isEqualTo(0.0);
        assertThat(evalNull.overallScore()).isEqualTo(0.0);
        assertThat(evalNull.correct()).isFalse();
        assertThat(evalNull.correctnessClassification()).isEqualTo("INCORRECT");
    }

    @Test
    @DisplayName("Completely evasive answer yields low score and INCORRECT without fake default 80")
    void testEvasiveAnswerYieldsLowScore() {
        InterviewQuestion q = createSampleQuestion();
        Job job = new Job();
        job.setTitle("Java Backend Developer");

        String evasiveAnswer = "I really like coding and I have done many great projects with my team in college.";
        EvaluatedAnswerDto eval = interviewAiService.evaluateAnswer(q, evasiveAnswer, 4.0, 15.0, job);

        // Technical score must not be a fake 80, 85, 90
        assertThat(eval.technicalScore()).isLessThan(45.0);
        assertThat(eval.overallScore()).isLessThan(50.0);
        assertThat(eval.correctnessClassification()).isEqualTo("INCORRECT");
        assertThat(eval.correct()).isFalse();
    }

    @Test
    @DisplayName("Answer addressing concepts yields score proportional to verified mechanisms")
    void testConceptMatchingYieldsProportionalScore() {
        InterviewQuestion q = createSampleQuestion();
        Job job = new Job();
        job.setTitle("Java Backend Developer");

        String partialAnswer = "The JVM manages memory via heap generations, dividing it into young generation and old generation. A minor GC cleans young generation using mark-sweep.";
        EvaluatedAnswerDto eval = interviewAiService.evaluateAnswer(q, partialAnswer, 3.0, 20.0, job);

        assertThat(eval.technicalScore()).isGreaterThan(30.0);
        assertThat(eval.overallScore()).isGreaterThan(20.0);
        assertThat(eval.overallScore()).isLessThanOrEqualTo(100.0);
        // Overall score must strictly match the weighted formula
        double expectedOverall = Math.round(((eval.technicalScore() * 0.45) + (eval.completenessScore() * 0.25) + (eval.clarityScore() * 0.15) + (eval.confidenceScore() * 0.15)) * 10.0) / 10.0;
        assertThat(eval.overallScore()).isEqualTo(expectedOverall);
    }
}
