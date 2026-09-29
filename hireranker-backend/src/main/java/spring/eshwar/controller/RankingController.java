package spring.eshwar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.eshwar.dto.ranking.RankingResponse;
import spring.eshwar.service.RankingService;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * Retrieves candidate ranking for a specific job.
     * Restricted to authenticated ADMIN users only.
     *
     * @param jobId the ID of the job
     * @return ranked list of candidate responses
     */
    @GetMapping("/jobs/{jobId}/ranking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RankingResponse>> getRankingForJob(@PathVariable Long jobId) {
        List<RankingResponse> rankings = rankingService.getRankingForJob(jobId);
        return ResponseEntity.ok(rankings);
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RankingResponse>> getAllRankings() {
        List<RankingResponse> rankings = rankingService.getAllRankings();
        return ResponseEntity.ok(rankings);
    }
}
