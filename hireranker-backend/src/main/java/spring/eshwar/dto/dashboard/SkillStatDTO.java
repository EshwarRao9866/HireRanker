package spring.eshwar.dto.dashboard;

public class SkillStatDTO {

    private String name;
    private long count;
    private double percent;
    private String cssClass;

    public SkillStatDTO() {
    }

    public SkillStatDTO(String name, long count, double percent, String cssClass) {
        this.name = name;
        this.count = count;
        this.percent = percent;
        this.cssClass = cssClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
