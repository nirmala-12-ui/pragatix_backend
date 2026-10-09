package jjcet.PragatiX.modules.admin.dto.report;

import java.time.LocalDate;

public class AcademicWeekOptionDto {
    private int weekNumber;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private String dateRangeLabel;

    public AcademicWeekOptionDto() {
    }

    public AcademicWeekOptionDto(int weekNumber, String label, LocalDate startDate, LocalDate endDate, String dateRangeLabel) {
        this.weekNumber = weekNumber;
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dateRangeLabel = dateRangeLabel;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDateRangeLabel() {
        return dateRangeLabel;
    }

    public void setDateRangeLabel(String dateRangeLabel) {
        this.dateRangeLabel = dateRangeLabel;
    }
}
