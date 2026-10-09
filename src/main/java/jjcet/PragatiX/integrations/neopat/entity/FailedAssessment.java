package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_assessment", indexes = {
        @Index(name = "idx_fail_email", columnList = "email"),
        @Index(name = "idx_fail_type", columnList = "failure_type"),
        @Index(name = "idx_fail_status", columnList = "status")
})
public class FailedAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_assessment_id")
    private Long sourceAssessmentId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "test_id", length = 100)
    private String testId;

    @Column(name = "marks", precision = 7, scale = 2)
    private BigDecimal marks;

    @Column(name = "total_marks", precision = 7, scale = 2)
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

    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;

    @Column(name = "failure_type", nullable = false, length = 100)
    private String failureType;

    @Column(name = "failure_reason", nullable = false, length = 1000)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "FAILED";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    public void onPrePersist() {
        java.time.ZoneId ist = java.time.ZoneId.of("Asia/Kolkata");
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ist);
        }
    }

    public FailedAssessment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceAssessmentId() {
        return sourceAssessmentId;
    }

    public void setSourceAssessmentId(Long sourceAssessmentId) {
        this.sourceAssessmentId = sourceAssessmentId;
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

    public String getFailureType() {
        return failureType;
    }

    public void setFailureType(String failureType) {
        this.failureType = failureType;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getLastRetryAt() {
        return lastRetryAt;
    }

    public void setLastRetryAt(LocalDateTime lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
