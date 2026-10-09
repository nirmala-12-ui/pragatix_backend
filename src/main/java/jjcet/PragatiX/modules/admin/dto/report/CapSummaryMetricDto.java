package jjcet.PragatiX.modules.admin.dto.report;

public class CapSummaryMetricDto {
    private int capIndex;
    private String label;
    private int awardedCount;
    private double percentage;

    public CapSummaryMetricDto() {
    }

    public CapSummaryMetricDto(int capIndex, String label, int awardedCount, double percentage) {
        this.capIndex = capIndex;
        this.label = label;
        this.awardedCount = awardedCount;
        this.percentage = percentage;
    }

    public int getCapIndex() {
        return capIndex;
    }

    public void setCapIndex(int capIndex) {
        this.capIndex = capIndex;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getAwardedCount() {
        return awardedCount;
    }

    public void setAwardedCount(int awardedCount) {
        this.awardedCount = awardedCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
