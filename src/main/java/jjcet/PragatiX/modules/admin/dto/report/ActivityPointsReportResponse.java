package jjcet.PragatiX.modules.admin.dto.report;

import java.util.List;

public class ActivityPointsReportResponse {
    private ActivityReportDetailDto activityDetails;
    private ReportSummaryDto summary;
    private List<DepartmentReportSummaryDto> departmentSummary;
    private List<CapColumnDto> capColumns;
    private List<StudentReportRowDto> students;
    private long totalStudentsCount;
    private int page;
    private int size;
    private int totalPages;
    private String activeTimeFilterLabel;

    public ActivityPointsReportResponse() {
    }

    public ActivityReportDetailDto getActivityDetails() {
        return activityDetails;
    }

    public void setActivityDetails(ActivityReportDetailDto activityDetails) {
        this.activityDetails = activityDetails;
    }

    public ReportSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(ReportSummaryDto summary) {
        this.summary = summary;
    }

    public List<DepartmentReportSummaryDto> getDepartmentSummary() {
        return departmentSummary;
    }

    public void setDepartmentSummary(List<DepartmentReportSummaryDto> departmentSummary) {
        this.departmentSummary = departmentSummary;
    }

    public List<CapColumnDto> getCapColumns() {
        return capColumns;
    }

    public void setCapColumns(List<CapColumnDto> capColumns) {
        this.capColumns = capColumns;
    }

    public List<StudentReportRowDto> getStudents() {
        return students;
    }

    public void setStudents(List<StudentReportRowDto> students) {
        this.students = students;
    }

    public long getTotalStudentsCount() {
        return totalStudentsCount;
    }

    public void setTotalStudentsCount(long totalStudentsCount) {
        this.totalStudentsCount = totalStudentsCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public String getActiveTimeFilterLabel() {
        return activeTimeFilterLabel;
    }

    public void setActiveTimeFilterLabel(String activeTimeFilterLabel) {
        this.activeTimeFilterLabel = activeTimeFilterLabel;
    }
}
