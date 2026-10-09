package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_history", indexes = {
        @Index(name = "idx_hist_email", columnList = "email"),
        @Index(name = "idx_hist_test_id", columnList = "test_id"),
        @Index(name = "idx_hist_sms_status", columnList = "sms_status")
})
public class AssessmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_assessment_id")
    private Long sourceAssessmentId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "test_id", nullable = false, length = 100)
    private String testId;

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

    @Column(name = "raw_payload", columnDefinition = "JSON")
    private String rawPayload;

    @Column(name = "sms_status", nullable = false, length = 50)
    private String smsStatus; // 'SENT' or 'FAILED'

    @Column(name = "sms_failure_reason", length = 500)
    private String smsFailureReason;

    @Column(name = "sms_sent_at")
    private LocalDateTime smsSentAt;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

    @PrePersist
    public void onPrePersist() {
        java.time.ZoneId ist = java.time.ZoneId.of("Asia/Kolkata");
        if (this.processedAt == null) {
            this.processedAt = LocalDateTime.now(ist);
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ist);
        }
    }

    public AssessmentHistory() {
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

    public String getSmsStatus() {
        return smsStatus;
    }

    public void setSmsStatus(String smsStatus) {
        this.smsStatus = smsStatus;
    }

    public String getSmsFailureReason() {
        return smsFailureReason;
    }

    public void setSmsFailureReason(String smsFailureReason) {
        this.smsFailureReason = smsFailureReason;
    }

    public LocalDateTime getSmsSentAt() {
        return smsSentAt;
    }

    public void setSmsSentAt(LocalDateTime smsSentAt) {
        this.smsSentAt = smsSentAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
