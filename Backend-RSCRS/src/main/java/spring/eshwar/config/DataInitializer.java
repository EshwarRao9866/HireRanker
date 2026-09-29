package spring.eshwar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.ApplicationStatus;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.JobStatus;
import spring.eshwar.entity.Resume;
import spring.eshwar.entity.Role;
import spring.eshwar.entity.User;
import spring.eshwar.repository.ApplicationRepository;
import spring.eshwar.repository.CandidateRepository;
import spring.eshwar.repository.JobRepository;
import spring.eshwar.repository.ResumeRepository;
import spring.eshwar.repository.UserRepository;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CandidateRepository candidateRepository,
                           JobRepository jobRepository,
                           ResumeRepository resumeRepository,
                           ApplicationRepository applicationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            initSeedData();
        } catch (Exception e) {
            log.warn("DataInitializer could not complete initialization (likely already seeded): {}", e.getMessage());
        }
    }

    private void initSeedData() {
        // 1. Admin User (Preserved for admin system access)
        if (userRepository.findByEmail("admin@hireranker.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@hireranker.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setName("Admin Recruiter");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Seeded default admin user: admin@hireranker.com");
        }

        log.info("DataInitializer completed: No demo/mock business data seeded.");
    }
}
