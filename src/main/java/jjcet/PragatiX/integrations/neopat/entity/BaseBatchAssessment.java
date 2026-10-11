package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseBatchAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "test_id", nullable = false, length = 100)
    private String testId;

    @Column(name = "test_name", length = 255)
    private String testName;

    @Column(name = "marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal marks;

    @Column(name = "total_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "attempts")
    private Integer attempts = 1;

    @Column(name = "result_analysis_url", length = 1024)
    private String resultAnalysisUrl;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "section_wise_marks", columnDefinition = "JSON")
    private String sectionWiseMarks;

    @Column(name = "processing_status", nullable = false, length = 50)
    private String processingStatus = "PENDING";

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

    public abstract Integer getBatchYear();

    @PrePersist
    public void onPrePersist() {
        java.time.ZoneId ist = java.time.ZoneId.of("Asia/Kolkata");
        if (this.receivedAt == null) {
            this.receivedAt = LocalDateTime.now(ist);
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ist);
        }
    }

    public BaseBatchAssessment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getResultAnalysisUrl() {
        return resultAnalysisUrl;
    }

    public void setResultAnalysisUrl(String resultAnalysisUrl) {
        this.resultAnalysisUrl = resultAnalysisUrl;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public String getSectionWiseMarks() {
        return sectionWiseMarks;
    }

    public void setSectionWiseMarks(String sectionWiseMarks) {
        this.sectionWiseMarks = sectionWiseMarks;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
