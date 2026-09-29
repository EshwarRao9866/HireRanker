package spring.eshwar.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing AIScreeningProvider delegating to the high-speed ResumeScreeningAIService via OpenRouter.
 */
@Component
public class OpenRouterScreeningProvider implements AIProvider, AIScreeningProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterScreeningProvider.class);

    private final ResumeScreeningAIService resumeScreeningAIService;

    public OpenRouterScreeningProvider(ResumeScreeningAIService resumeScreeningAIService) {
        this.resumeScreeningAIService = resumeScreeningAIService;
    }

    @Override
    public ScreeningEvaluationResult evaluate(ScreeningContext context) {
        return resumeScreeningAIService.evaluateResume(context);
    }

    @Override
    public String getProviderName() {
        return "openrouter";
    }

    public ScreeningEvaluationResult evaluateFallback(ScreeningContext ctx, String reason) {
        return resumeScreeningAIService.evaluateFallback(ctx, reason);
    }
}
