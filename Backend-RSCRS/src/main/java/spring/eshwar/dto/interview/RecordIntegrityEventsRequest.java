package spring.eshwar.dto.interview;

import java.util.List;

public class RecordIntegrityEventsRequest {

    private List<IntegrityEventItem> events;

    public RecordIntegrityEventsRequest() {
    }

    public RecordIntegrityEventsRequest(List<IntegrityEventItem> events) {
        this.events = events;
    }

    public List<IntegrityEventItem> getEvents() {
        return events;
    }

    public void setEvents(List<IntegrityEventItem> events) {
        this.events = events;
    }

    public static class IntegrityEventItem {
        private String eventType;
        private String severity;
        private Long startTime;
        private Long endTime;
        private Double durationSeconds;
        private String message;

        public IntegrityEventItem() {
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public Long getStartTime() {
            return startTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }

        public Long getEndTime() {
            return endTime;
        }

        public void setEndTime(Long endTime) {
            this.endTime = endTime;
        }

        public Double getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(Double durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
