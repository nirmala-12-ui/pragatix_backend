package jjcet.PragatiX.integrations.neopat.dto;

import java.time.LocalDateTime;

public class ParentContactResponseDto {

    private Long id;
    private String studentEmail;
    private String parentMobile;
    private String parentMobileMasked;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ParentContactResponseDto() {
    }

    public ParentContactResponseDto(Long id, String studentEmail, String parentMobileMasked, Boolean isActive,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, studentEmail, null, parentMobileMasked, isActive, createdAt, updatedAt);
    }

    public ParentContactResponseDto(Long id, String studentEmail, String parentMobile, String parentMobileMasked,
                                    Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentEmail = studentEmail;
        this.parentMobile = parentMobile;
        this.parentMobileMasked = parentMobileMasked;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getParentMobileMasked() {
        return parentMobileMasked;
    }

    public void setParentMobileMasked(String parentMobileMasked) {
        this.parentMobileMasked = parentMobileMasked;
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

    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return "N/A";
        }
        String clean = mobile.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        int keep = 4;
        String last4 = clean.substring(clean.length() - keep);
        return "*".repeat(Math.max(4, clean.length() - keep)) + last4;
    }
}
