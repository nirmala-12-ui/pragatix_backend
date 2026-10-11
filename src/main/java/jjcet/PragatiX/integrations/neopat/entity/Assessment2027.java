package jjcet.PragatiX.integrations.neopat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_2027", indexes = {
        @Index(name = "idx_a2027_status", columnList = "processing_status"),
        @Index(name = "idx_a2027_email", columnList = "email"),
        @Index(name = "idx_a2027_test_id", columnList = "test_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_a2027_email_test", columnNames = {"email", "test_id"})
})
public class Assessment2027 extends BaseBatchAssessment {

    public Assessment2027() {
        super();
    }

    @Override
    public Integer getBatchYear() {
        return 2027;
    }
}
