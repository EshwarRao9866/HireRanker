package spring.eshwar.dto.dashboard;

import java.util.ArrayList;
import java.util.List;

public class ApplicantsOverviewDTO {

    private long totalApplicants;
    private long screenedResumes;
    private long shortlisted;
    private long interviewsScheduled;
    private List<String> labels = new ArrayList<>();
    private List<Long> applicationsSeries = new ArrayList<>();
    private List<Long> screenedSeries = new ArrayList<>();

    public ApplicantsOverviewDTO() {
    }

    public ApplicantsOverviewDTO(long totalApplicants, long screenedResumes, long shortlisted,
                                 long interviewsScheduled, List<String> labels,
                                 List<Long> applicationsSeries, List<Long> screenedSeries) {
        this.totalApplicants = totalApplicants;
        this.screenedResumes = screenedResumes;
        this.shortlisted = shortlisted;
        this.interviewsScheduled = interviewsScheduled;
        this.labels = labels != null ? labels : new ArrayList<>();
        this.applicationsSeries = applicationsSeries != null ? applicationsSeries : new ArrayList<>();
        this.screenedSeries = screenedSeries != null ? screenedSeries : new ArrayList<>();
    }

    public long getTotalApplicants() {
        return totalApplicants;
    }

    public void setTotalApplicants(long totalApplicants) {
        this.totalApplicants = totalApplicants;
    }

    public long getScreenedResumes() {
        return screenedResumes;
    }

    public void setScreenedResumes(long screenedResumes) {
        this.screenedResumes = screenedResumes;
    }

    public long getShortlisted() {
        return shortlisted;
    }

    public void setShortlisted(long shortlisted) {
        this.shortlisted = shortlisted;
    }

    public long getInterviewsScheduled() {
        return interviewsScheduled;
    }

    public void setInterviewsScheduled(long interviewsScheduled) {
        this.interviewsScheduled = interviewsScheduled;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Long> getApplicationsSeries() {
        return applicationsSeries;
    }

    public void setApplicationsSeries(List<Long> applicationsSeries) {
        this.applicationsSeries = applicationsSeries;
    }

    public List<Long> getScreenedSeries() {
        return screenedSeries;
    }

    public void setScreenedSeries(List<Long> screenedSeries) {
        this.screenedSeries = screenedSeries;
    }
}
