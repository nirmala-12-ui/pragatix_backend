package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_2030", indexes = {
        @Index(name = "idx_a2030_status", columnList = "processing_status"),
        @Index(name = "idx_a2030_email", columnList = "email"),
        @Index(name = "idx_a2030_test_id", columnList = "test_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_a2030_email_test", columnNames = {"email", "test_id"})
})
public class Assessment2030 extends BaseBatchAssessment {

    public Assessment2030() {
        super();
    }

    @Override
    public Integer getBatchYear() {
        return 2030;
    }
}
