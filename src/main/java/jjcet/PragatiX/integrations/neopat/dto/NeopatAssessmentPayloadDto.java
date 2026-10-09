package jjcet.PragatiX.integrations.neopat.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeopatAssessmentPayloadDto {

    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Test ID is required")
    @JsonProperty("test_id")
    @JsonAlias({"test_id", "testId", "assessmentname", "assessment_name", "assessment_id"})
    private String testId;

    @NotNull(message = "Marks obtained is required")
    @PositiveOrZero(message = "Marks must be non-negative")
    @JsonProperty("marks")
    @JsonAlias({"marks", "marksscored", "score"})
    private BigDecimal marks;

    @NotNull(message = "Total marks is required")
    @PositiveOrZero(message = "Total marks must be non-negative")
    @JsonProperty("total_marks")
    @JsonAlias({"total_marks", "totalmark", "max_marks"})
    private BigDecimal totalMarks;

    private Integer attempts = 1;

    @JsonProperty("result_analysis_url")
    private String resultAnalysisUrl;

    @JsonProperty("starttime")
    @JsonAlias({"start_time", "starttime"})
    private LocalDateTime startTime;

    @JsonProperty("submittime")
    @JsonAlias({"submit_time", "submittime"})
    private LocalDateTime submitTime;

    @JsonProperty("section_wise_marks")
    private Object sectionWiseMarks;

    public NeopatAssessmentPayloadDto() {
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

    @JsonSetter("starttime")
    public void setStartTime(Object raw) {
        if (raw == null) {
            this.startTime = null;
        } else if (raw instanceof LocalDateTime ldt) {
            this.startTime = ldt;
        } else {
            this.startTime = NeopatDateTimeUtil.parseAsUtc(raw.toString());
        }
    }

    @JsonSetter("start_time")
    public void setStartTimeLegacy(Object raw) {
        setStartTime(raw);
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    @JsonSetter("submittime")
    public void setSubmitTime(Object raw) {
        if (raw == null) {
            this.submitTime = null;
        } else if (raw instanceof LocalDateTime ldt) {
            this.submitTime = ldt;
        } else {
            this.submitTime = NeopatDateTimeUtil.parseAsUtc(raw.toString());
        }
    }

    @JsonSetter("submit_time")
    public void setSubmitTimeLegacy(Object raw) {
        setSubmitTime(raw);
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public Object getSectionWiseMarks() {
        return sectionWiseMarks;
    }

    public void setSectionWiseMarks(Object sectionWiseMarks) {
        this.sectionWiseMarks = sectionWiseMarks;
    }
}
