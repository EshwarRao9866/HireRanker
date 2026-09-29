package spring.eshwar.dto.dashboard;

public class StatusBreakdownDTO {

    private String status;
    private String label;
    private long count;
    private double percent;
    private String cssClass;

    public StatusBreakdownDTO() {
    }

    public StatusBreakdownDTO(String status, String label, long count, double percent, String cssClass) {
        this.status = status;
        this.label = label;
        this.count = count;
        this.percent = percent;
        this.cssClass = cssClass;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }
}
