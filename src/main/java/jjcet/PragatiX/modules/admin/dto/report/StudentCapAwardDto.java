package jjcet.PragatiX.modules.admin.dto.report;

public class StudentCapAwardDto {
    private int capNumber;
    private Integer points; // null if Fixed XP or not awarded
    private String awardedAt;
    private String awardedBy;
    private String remarks;
    private String status; // "Awarded", "Penalized", "Not Awarded"
    private boolean isPenalized;

    public StudentCapAwardDto() {
    }

    public StudentCapAwardDto(int capNumber, Integer points, String awardedAt, String awardedBy, String remarks) {
        this.capNumber = capNumber;
        this.points = points;
        this.awardedAt = awardedAt;
        this.awardedBy = awardedBy;
        this.remarks = remarks;
        this.status = (points != null && points < 0) ? "Penalized" : ((awardedAt != null && !"-".equals(awardedAt)) ? "Awarded" : "Not Awarded");
        this.isPenalized = (points != null && points < 0);
    }

    public StudentCapAwardDto(int capNumber, Integer points, String awardedAt, String awardedBy, String remarks, String status, boolean isPenalized) {
        this.capNumber = capNumber;
        this.points = points;
        this.awardedAt = awardedAt;
        this.awardedBy = awardedBy;
        this.remarks = remarks;
        this.status = status;
        this.isPenalized = isPenalized;
    }

    public int getCapNumber() {
        return capNumber;
    }

    public void setCapNumber(int capNumber) {
        this.capNumber = capNumber;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(String awardedAt) {
        this.awardedAt = awardedAt;
    }

    public String getAwardedBy() {
        return awardedBy;
    }

    public void setAwardedBy(String awardedBy) {
        this.awardedBy = awardedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isPenalized")
    public boolean isPenalized() {
        return isPenalized;
    }

    public void setPenalized(boolean penalized) {
        this.isPenalized = penalized;
    }
}
