package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "parent_contact", indexes = {
        @Index(name = "idx_parent_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_parent_student_email", columnNames = {"student_email"})
})
public class ParentContact {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_email", nullable = false, unique = true)
    private String studentEmail;

    @Column(name = "parent_mobile", nullable = false, length = 20)
    private String parentMobile;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(IST);

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now(IST);

    public ParentContact() {
    }

    public ParentContact(String studentEmail, String parentMobile) {
        this.studentEmail = studentEmail;
        this.parentMobile = parentMobile;
        this.isActive = true;
        this.createdAt = LocalDateTime.now(IST);
        this.updatedAt = LocalDateTime.now(IST);
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(IST);
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now(IST);
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(IST);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getParentMobile() {
        return parentMobile;
    }

    public void setParentMobile(String parentMobile) {
        this.parentMobile = parentMobile;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
