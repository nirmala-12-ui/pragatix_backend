package jjcet.PragatiX.integrations.neopat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ParentContactRequestDto {

    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid student email format")
    private String studentEmail;

    @NotBlank(message = "Parent mobile number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Parent mobile must be between 10 and 15 digits, optionally prefixed with '+'")
    private String parentMobile;

    private Boolean isActive = true;

    public ParentContactRequestDto() {
    }

    public ParentContactRequestDto(String studentEmail, String parentMobile) {
        this.studentEmail = studentEmail;
        this.parentMobile = parentMobile;
        this.isActive = true;
    }

    public ParentContactRequestDto(String studentEmail, String parentMobile, Boolean isActive) {
        this.studentEmail = studentEmail;
        this.parentMobile = parentMobile;
        this.isActive = isActive != null ? isActive : true;
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
}
