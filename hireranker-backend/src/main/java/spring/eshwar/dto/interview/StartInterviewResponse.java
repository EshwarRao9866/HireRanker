package spring.eshwar.dto.interview;

public class StartInterviewResponse {

    private Long interviewId;
    private Long applicationId;
    private String candidateName;
    private String jobTitle;
    private String status;
    private int totalQuestionsTarget;
    private LiveQuestionResponse firstQuestion;
    private String introduction;

    public StartInterviewResponse() {
    }

    public StartInterviewResponse(Long interviewId, Long applicationId, String candidateName,
                                  String jobTitle, String status, int totalQuestionsTarget,
                                  LiveQuestionResponse firstQuestion, String introduction) {
        this.interviewId = interviewId;
        this.applicationId = applicationId;
        this.candidateName = candidateName;
        this.jobTitle = jobTitle;
        this.status = status;
        this.totalQuestionsTarget = totalQuestionsTarget;
        this.firstQuestion = firstQuestion;
        this.introduction = introduction;
    }

    public StartInterviewResponse(Long interviewId, Long applicationId, String candidateName,
                                  String jobTitle, String status, int totalQuestionsTarget,
                                  LiveQuestionResponse firstQuestion) {
        this(interviewId, applicationId, candidateName, jobTitle, status, totalQuestionsTarget, firstQuestion, null);
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalQuestionsTarget() {
        return totalQuestionsTarget;
    }

    public void setTotalQuestionsTarget(int totalQuestionsTarget) {
        this.totalQuestionsTarget = totalQuestionsTarget;
    }

    public LiveQuestionResponse getFirstQuestion() {
        return firstQuestion;
    }

    public void setFirstQuestion(LiveQuestionResponse firstQuestion) {
        this.firstQuestion = firstQuestion;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }
}
