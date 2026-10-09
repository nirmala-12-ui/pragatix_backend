package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "neopat_sms_execution", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exec_date_time", columnNames = {"scheduled_date", "scheduled_time"})
})
public class NeopatSmsExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // 'IN_PROGRESS', 'COMPLETED', 'FAILED'

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

    public NeopatSmsExecution() {
    }

    public NeopatSmsExecution(LocalDate scheduledDate, LocalTime scheduledTime, String status) {
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.status = status;
        this.startedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        this.createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
    }

    @PrePersist
    public void onPrePersist() {
        java.time.ZoneId ist = java.time.ZoneId.of("Asia/Kolkata");
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now(ist);
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ist);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
