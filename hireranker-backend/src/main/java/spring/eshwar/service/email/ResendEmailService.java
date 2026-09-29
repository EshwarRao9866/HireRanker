package spring.eshwar.service.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import spring.eshwar.entity.Application;
import spring.eshwar.entity.Candidate;
import spring.eshwar.entity.Interview;
import spring.eshwar.entity.Job;
import spring.eshwar.entity.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to send transactional notification emails via Resend HTTP API.
 * Endpoint: POST https://api.resend.com/emails
 * Never throws exceptions upward to avoid rolling back database transactions.
 * Never logs or exposes the Resend API key or sensitive tokens.
 */
@Service
public class ResendEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${resend.api.key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${resend.sender.email:${RESEND_SENDER_EMAIL:}}")
    private String senderEmail;

    @Value("${resend.sender.name:${RESEND_SENDER_NAME:HireRanker}}")
    private String senderName;

    @Value("${resend.timeout-seconds:10}")
    private int resendTimeoutSeconds;

    public ResendEmailService(RestClient restClient, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves the candidate's existing email through Application -> Candidate -> User.
     * Returns null if any relation is null or email is blank.
     */
    public String extractCandidateEmail(Application application) {
        if (application == null) {
            return null;
        }
        Candidate candidate = application.getCandidate();
        if (candidate == null) {
            return null;
        }
        User user = candidate.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return null;
        }
        return user.getEmail().trim();
    }

    /**
     * Resolves candidate display name.
     */
    public String extractCandidateName(Application application) {
        if (application == null) {
            return "Candidate";
        }
        Candidate candidate = application.getCandidate();
        if (candidate != null && candidate.getFullName() != null && !candidate.getFullName().isBlank()) {
            return candidate.getFullName().trim();
        }
        if (candidate != null && candidate.getUser() != null && candidate.getUser().getName() != null && !candidate.getUser().getName().isBlank()) {
            return candidate.getUser().getName().trim();
        }
        return "Candidate";
    }

    /**
     * Sends shortlist notification email to candidate.
     */
    public boolean sendShortlistedEmail(Application application) {
        if (application == null) {
            logger.warn("Cannot send shortlist email: Application is null.");
            return false;
        }

        String recipientEmail = extractCandidateEmail(application);
        String candidateName = extractCandidateName(application);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.info("Application id {} status updated successfully, but candidate email is missing.", application.getId());
            return false;
        }

        Job job = application.getJob();
        String jobTitle = (job != null && job.getTitle() != null && !job.getTitle().isBlank())
                ? job.getTitle().trim()
                : "the requested position";
        String companyName = (job != null && job.getCompany() != null && !job.getCompany().isBlank())
                ? job.getCompany().trim()
                : "our organization";

        String subject = "Congratulations! Your Application Has Been Shortlisted";

        String textContent = "Hello " + candidateName + ",\n\n"
                + "We are pleased to inform you that your application for "
                + jobTitle + " at " + companyName + " has been shortlisted.\n\n"
                + "Our recruitment team will contact you with the next steps.\n\n"
                + "Regards,\n"
                + "HireRanker Recruitment Team";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">"
                + "<p>Hello <strong>" + escapeHtml(candidateName) + "</strong>,</p>"
                + "<p>We are pleased to inform you that your application for <strong>" + escapeHtml(jobTitle)
                + "</strong> at <strong>" + escapeHtml(companyName) + "</strong> has been shortlisted.</p>"
                + "<p>Our recruitment team will contact you with the next steps.</p>"
                + "<br/>"
                + "<p>Regards,<br/><strong>HireRanker Recruitment Team</strong></p>"
                + "</div>";

        boolean sent = sendTransactionalEmail(recipientEmail, candidateName, subject, textContent, htmlContent);
        if (sent) {
            logger.info("Shortlist notification email successfully dispatched to candidate for application id: {}", application.getId());
        } else {
            logger.warn("Candidate shortlisted successfully, but notification email failed for application id: {}", application.getId());
        }
        return sent;
    }

    /**
     * Sends rejection notification email to candidate.
     */
    public boolean sendRejectedEmail(Application application) {
        if (application == null) {
            logger.warn("Cannot send rejection email: Application is null.");
            return false;
        }

        String recipientEmail = extractCandidateEmail(application);
        String candidateName = extractCandidateName(application);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.info("Application status updated successfully, but candidate email is missing for application id: {}", application.getId());
            return false;
        }

        Job job = application.getJob();
        String jobTitle = (job != null && job.getTitle() != null && !job.getTitle().isBlank())
                ? job.getTitle().trim()
                : "the requested position";
        String companyName = (job != null && job.getCompany() != null && !job.getCompany().isBlank())
                ? job.getCompany().trim()
                : "our organization";

        String subject = "Update Regarding Your Job Application";

        String textContent = "Hello " + candidateName + ",\n\n"
                + "Thank you for your interest in the " + jobTitle + " position at " + companyName + ".\n\n"
                + "After reviewing your application, we will not be proceeding with your application at this stage.\n\n"
                + "We appreciate the time and effort you invested in the process.\n\n"
                + "Regards,\n"
                + "HireRanker Recruitment Team";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">"
                + "<p>Hello <strong>" + escapeHtml(candidateName) + "</strong>,</p>"
                + "<p>Thank you for your interest in the <strong>" + escapeHtml(jobTitle)
                + "</strong> position at <strong>" + escapeHtml(companyName) + "</strong>.</p>"
                + "<p>After reviewing your application, we will not be proceeding with your application at this stage.</p>"
                + "<p>We appreciate the time and effort you invested in the process.</p>"
                + "<br/>"
                + "<p>Regards,<br/><strong>HireRanker Recruitment Team</strong></p>"
                + "</div>";

        boolean sent = sendTransactionalEmail(recipientEmail, candidateName, subject, textContent, htmlContent);
        if (sent) {
            logger.info("Rejection notification email successfully dispatched to candidate for application id: {}", application.getId());
        } else {
            logger.warn("Application status updated successfully, but rejection notification email failed for application id: {}", application.getId());
        }
        return sent;
    }

    /**
     * Sends interview scheduled email to candidate.
     */
    public boolean sendInterviewScheduledEmail(Interview interview) {
        if (interview == null || interview.getApplication() == null) {
            logger.warn("Cannot send interview scheduled email: Interview or linked application is null.");
            return false;
        }

        Application application = interview.getApplication();
        String recipientEmail = extractCandidateEmail(application);
        String candidateName = extractCandidateName(application);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.info("Interview scheduled successfully, but candidate email is missing for interview id: {}", interview.getId());
            return false;
        }

        Job job = application.getJob();
        String jobTitle = (job != null && job.getTitle() != null && !job.getTitle().isBlank())
                ? job.getTitle().trim()
                : "Position";
        String companyName = (job != null && job.getCompany() != null && !job.getCompany().isBlank())
                ? job.getCompany().trim()
                : "HireRanker Partner";

        LocalDateTime scheduledAt = interview.getScheduledDateTime();
        String dateStr = scheduledAt != null ? scheduledAt.format(DATE_FORMATTER) : "To Be Decided";
        String timeStr = scheduledAt != null ? scheduledAt.format(TIME_FORMATTER) : "To Be Decided";
        String modeStr = interview.getInterviewType() != null ? interview.getInterviewType() : "ONLINE";
        String meetingLink = interview.getMeetingLink() != null ? interview.getMeetingLink().trim() : "";
        String notes = interview.getNotes() != null ? interview.getNotes().trim() : "";

        String subject = "Interview Scheduled - " + jobTitle;

        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("Hello ").append(candidateName).append(",\n\n");
        textBuilder.append("Your interview for the ").append(jobTitle).append(" position at ").append(companyName).append(" has been scheduled.\n\n");
        textBuilder.append("Date: ").append(dateStr).append("\n");
        textBuilder.append("Time: ").append(timeStr).append("\n");
        textBuilder.append("Mode: ").append(modeStr).append("\n");

        if (!meetingLink.isBlank()) {
            textBuilder.append("Meeting Link: ").append(meetingLink).append("\n");
        }
        if (!notes.isBlank()) {
            textBuilder.append("Instructions: ").append(notes).append("\n");
        }
        textBuilder.append("\nPlease be available at the scheduled time.\n\n");
        textBuilder.append("Regards,\nHireRanker Recruitment Team");

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">");
        htmlBuilder.append("<p>Hello <strong>").append(escapeHtml(candidateName)).append("</strong>,</p>");
        htmlBuilder.append("<p>Your interview for the <strong>").append(escapeHtml(jobTitle)).append("</strong> position at <strong>")
                .append(escapeHtml(companyName)).append("</strong> has been scheduled.</p>");
        htmlBuilder.append("<ul>");
        htmlBuilder.append("<li><strong>Date:</strong> ").append(escapeHtml(dateStr)).append("</li>");
        htmlBuilder.append("<li><strong>Time:</strong> ").append(escapeHtml(timeStr)).append("</li>");
        htmlBuilder.append("<li><strong>Mode:</strong> ").append(escapeHtml(modeStr)).append("</li>");
        if (!meetingLink.isBlank()) {
            htmlBuilder.append("<li><strong>Meeting Link:</strong> <a href=\"").append(escapeHtml(meetingLink))
                    .append("\">").append(escapeHtml(meetingLink)).append("</a></li>");
        }
        if (!notes.isBlank()) {
            htmlBuilder.append("<li><strong>Instructions:</strong> ").append(escapeHtml(notes)).append("</li>");
        }
        htmlBuilder.append("</ul>");
        htmlBuilder.append("<p>Please be available at the scheduled time.</p><br/>");
        htmlBuilder.append("<p>Regards,<br/><strong>HireRanker Recruitment Team</strong></p>");
        htmlBuilder.append("</div>");

        boolean sent = sendTransactionalEmail(recipientEmail, candidateName, subject, textBuilder.toString(), htmlBuilder.toString());
        if (sent) {
            logger.info("Interview scheduled notification email successfully dispatched to candidate for interview id: {}", interview.getId());
        } else {
            logger.warn("Interview scheduled successfully, but notification email could not be sent for interview id: {}", interview.getId());
        }
        return sent;
    }

    /**
     * Core reusable method dispatching the email via Resend HTTP API.
     * Endpoint: POST https://api.resend.com/emails
     * Safely catches any exception without propagating upward.
     */
    public boolean sendTransactionalEmail(String recipientEmail, String recipientName, String subject, String textContent, String htmlContent) {
        if (resendApiKey == null || resendApiKey.trim().isBlank()) {
            logger.warn("[EMAIL] Provider: Resend | API key is missing. Skipping email notification to: {}", maskEmail(recipientEmail));
            return false;
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.warn("[EMAIL] Provider: Resend | Recipient email is blank. Skipping email notification.");
            return false;
        }

        String effectiveSenderEmail = (senderEmail != null && !senderEmail.trim().isBlank())
                ? senderEmail.trim()
                : "onboarding@resend.dev";
        String effectiveSenderName = (senderName != null && !senderName.trim().isBlank())
                ? senderName.trim()
                : "HireRanker";

        // Format sender as "Sender Name <sender@example.com>"
        String fromHeader = String.format("%s <%s>", effectiveSenderName, effectiveSenderEmail);

        try {
            logger.info("[EMAIL] Provider: Resend | Preparing email for recipient: {}", maskEmail(recipientEmail));

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", fromHeader);

            List<String> toList = new ArrayList<>();
            toList.add(recipientEmail.trim());
            payload.put("to", toList);

            payload.put("subject", subject);
            if (htmlContent != null && !htmlContent.isBlank()) {
                payload.put("html", htmlContent);
            }
            if (textContent != null && !textContent.isBlank()) {
                payload.put("text", textContent);
            }

            logger.info("[EMAIL] Calling Resend API: {}", RESEND_EMAILS_URL);

            var responseEntity = restClient.post()
                    .uri(RESEND_EMAILS_URL)
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(JsonNode.class);

            int statusCode = responseEntity.getStatusCode().value();
            logger.info("[EMAIL] Resend response status: {}", statusCode);

            JsonNode responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.has("id")) {
                String emailId = responseBody.get("id").asText();
                logger.info("[EMAIL] Email accepted by Resend. Resend id: {}, recipient: {}", emailId, maskEmail(recipientEmail));
                return true;
            } else {
                logger.info("[EMAIL] Email accepted by Resend (Status {}), recipient: {}", statusCode, maskEmail(recipientEmail));
                return true;
            }
        } catch (HttpClientErrorException ex) {
            int status = ex.getStatusCode().value();
            logger.error("[EMAIL] Resend request failed. HTTP status: {}. Reason: {}", status, ex.getStatusText());
            return false;
        } catch (HttpServerErrorException ex) {
            logger.error("[EMAIL] Resend server error. HTTP status: {}. Reason: {}", ex.getStatusCode().value(), ex.getStatusText());
            return false;
        } catch (ResourceAccessException ex) {
            logger.error("[EMAIL] Resend connection/timeout failure: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            // Never log sensitive credentials or api-key
            logger.error("[EMAIL] Failed to dispatch email via Resend to {}: {}", maskEmail(recipientEmail), ex.getMessage());
            return false;
        }
    }

    /**
     * Sends password reset transactional email via Resend asynchronously.
     * Offloaded from the web request thread so the frontend gets an instant response.
     */
    @Async
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetLink) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.warn("Cannot send password reset email: Recipient email is blank.");
            return;
        }

        String displayName = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "User";
        String subject = "Reset Your HireRanker Password";

        String textContent = "Hello " + displayName + ",\n\n"
                + "We received a request to reset your HireRanker password.\n\n"
                + "Click the link below to create a new password:\n"
                + resetLink + "\n\n"
                + "This link will expire in 30 minutes.\n\n"
                + "If you did not request a password reset, you can safely ignore this email.\n\n"
                + "Regards,\n"
                + "HireRanker Team";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #1e293b; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px;\">"
                + "<div style=\"margin-bottom: 20px;\">"
                + "<h2 style=\"color: #4f46e5; margin: 0;\">HireRanker</h2>"
                + "<p style=\"color: #64748b; margin: 4px 0 0;\">Password Reset Request</p>"
                + "</div>"
                + "<p>Hello <strong>" + escapeHtml(displayName) + "</strong>,</p>"
                + "<p>We received a request to reset your HireRanker password.</p>"
                + "<p>Click the button below to create a new password:</p>"
                + "<div style=\"text-align: center; margin: 30px 0;\">"
                + "<a href=\"" + escapeHtml(resetLink) + "\" style=\"background-color: #4f46e5; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: 600; display: inline-block;\">Reset Password</a>"
                + "</div>"
                + "<p style=\"color: #64748b; font-size: 13px;\">Or copy and paste this link in your browser:<br/><a href=\"" + escapeHtml(resetLink) + "\" style=\"color: #4f46e5; word-break: break-all;\">" + escapeHtml(resetLink) + "</a></p>"
                + "<p style=\"color: #ef4444; font-size: 13px;\">⚠️ This link will expire in 30 minutes and can only be used once.</p>"
                + "<p style=\"color: #64748b; font-size: 13px;\">If you did not request a password reset, you can safely ignore this email.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                + "<p style=\"color: #64748b; font-size: 13px;\">Regards,<br/><strong>HireRanker Team</strong></p>"
                + "</div>";

        boolean sent = sendTransactionalEmail(recipientEmail, displayName, subject, textContent, htmlContent);
        if (sent) {
            logger.info("Password reset email dispatched via Resend to {}", maskEmail(recipientEmail));
        } else {
            logger.warn("Failed to dispatch password reset email via Resend to {}", maskEmail(recipientEmail));
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[empty]";
        }
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) {
            return "***" + (atIdx >= 0 ? email.substring(atIdx) : "");
        }
        return email.charAt(0) + "***" + email.substring(atIdx - 1);
    }

    private String escapeHtml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
