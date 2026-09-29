package spring.eshwar.service.ai;

import spring.eshwar.service.ai.AIScreeningProvider.ScreeningContext;
import spring.eshwar.service.ai.AIScreeningProvider.ScreeningEvaluationResult;

/**
 * Pluggable AI provider abstraction for HireRanker.
 * Decouples resume screening business logic from specific AI models and vendors (OpenRouter, Groq, local models, etc.).
 */
public interface AIProvider {

    /**
     * Evaluates a candidate resume against job requirements and evaluation criteria.
     *
     * @param context the screening context containing job description, required skills, and resume text
     * @return the structured evaluation result
     */
    ScreeningEvaluationResult evaluate(ScreeningContext context);

    /**
     * Evaluates a candidate resume using deterministic fallback heuristic.
     */
    default ScreeningEvaluationResult evaluateFallback(ScreeningContext context, String reason) {
        return evaluate(context);
    }

    /**
     * Provider identification name (e.g., "openrouter", "groq", "heuristic").
     */
    String getProviderName();
}
