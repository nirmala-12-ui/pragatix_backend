package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_2029", indexes = {
        @Index(name = "idx_a2029_status", columnList = "processing_status"),
        @Index(name = "idx_a2029_email", columnList = "email"),
        @Index(name = "idx_a2029_test_id", columnList = "test_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_a2029_email_test", columnNames = {"email", "test_id"})
})
public class Assessment2029 extends BaseBatchAssessment {

    public Assessment2029() {
        super();
    }

    @Override
    public Integer getBatchYear() {
        return 2029;
    }
}
