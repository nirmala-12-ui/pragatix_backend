package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_2028", indexes = {
        @Index(name = "idx_a2028_status", columnList = "processing_status"),
        @Index(name = "idx_a2028_email", columnList = "email"),
        @Index(name = "idx_a2028_test_id", columnList = "test_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_a2028_email_test", columnNames = {"email", "test_id"})
})
public class Assessment2028 extends BaseBatchAssessment {

    public Assessment2028() {
        super();
    }

    @Override
    public Integer getBatchYear() {
        return 2028;
    }
}
