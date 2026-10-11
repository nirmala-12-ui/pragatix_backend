package jjcet.PragatiX.integrations.neopat.service;

import jjcet.PragatiX.integrations.neopat.entity.*;
import jjcet.PragatiX.integrations.neopat.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Type-safe routing provider mapping supported batch years (2027, 2028, 2029, 2030)
 * directly to their respective physical table repositories and entity implementations.
 */
@Service
public class BatchAssessmentRepositoryProvider {

    private final Assessment2027Repository repo2027;
    private final Assessment2028Repository repo2028;
    private final Assessment2029Repository repo2029;
    private final Assessment2030Repository repo2030;

    public BatchAssessmentRepositoryProvider(Assessment2027Repository repo2027,
                                             Assessment2028Repository repo2028,
                                             Assessment2029Repository repo2029,
                                             Assessment2030Repository repo2030) {
        this.repo2027 = repo2027;
        this.repo2028 = repo2028;
        this.repo2029 = repo2029;
        this.repo2030 = repo2030;
    }

    public BaseBatchAssessment createNewEntity(int batchYear) {
        return switch (batchYear) {
            case 2027 -> new Assessment2027();
            case 2028 -> new Assessment2028();
            case 2029 -> new Assessment2029();
            case 2030 -> new Assessment2030();
            default -> throw new IllegalArgumentException("Unsupported batch year: " + batchYear);
        };
    }

    public Optional<? extends BaseBatchAssessment> findByBatchAndEmailAndTestId(int batchYear, String email, String testId) {
        return switch (batchYear) {
            case 2027 -> repo2027.findByEmailAndTestId(email, testId);
            case 2028 -> repo2028.findByEmailAndTestId(email, testId);
            case 2029 -> repo2029.findByEmailAndTestId(email, testId);
            case 2030 -> repo2030.findByEmailAndTestId(email, testId);
            default -> Optional.empty();
        };
    }

    public BaseBatchAssessment save(BaseBatchAssessment entity) {
        if (entity instanceof Assessment2027 a27) {
            return repo2027.save(a27);
        } else if (entity instanceof Assessment2028 a28) {
            return repo2028.save(a28);
        } else if (entity instanceof Assessment2029 a29) {
            return repo2029.save(a29);
        } else if (entity instanceof Assessment2030 a30) {
            return repo2030.save(a30);
        }
        throw new IllegalArgumentException("Unknown assessment entity type: " + entity.getClass().getName());
    }

    public List<? extends BaseBatchAssessment> findPendingByBatch(int batchYear) {
        return switch (batchYear) {
            case 2027 -> repo2027.findByProcessingStatus("PENDING");
            case 2028 -> repo2028.findByProcessingStatus("PENDING");
            case 2029 -> repo2029.findByProcessingStatus("PENDING");
            case 2030 -> repo2030.findByProcessingStatus("PENDING");
            default -> List.of();
        };
    }

    public long countPendingByBatch(int batchYear) {
        return switch (batchYear) {
            case 2027 -> repo2027.countByProcessingStatus("PENDING");
            case 2028 -> repo2028.countByProcessingStatus("PENDING");
            case 2029 -> repo2029.countByProcessingStatus("PENDING");
            case 2030 -> repo2030.countByProcessingStatus("PENDING");
            default -> 0L;
        };
    }

    public void delete(BaseBatchAssessment entity) {
        if (entity instanceof Assessment2027 a27) {
            repo2027.delete(a27);
        } else if (entity instanceof Assessment2028 a28) {
            repo2028.delete(a28);
        } else if (entity instanceof Assessment2029 a29) {
            repo2029.delete(a29);
        } else if (entity instanceof Assessment2030 a30) {
            repo2030.delete(a30);
        }
    }

    public List<? extends BaseBatchAssessment> findAllPendingAcrossAllBatches() {
        List<BaseBatchAssessment> all = new ArrayList<>();
        all.addAll(repo2027.findByProcessingStatus("PENDING"));
        all.addAll(repo2028.findByProcessingStatus("PENDING"));
        all.addAll(repo2029.findByProcessingStatus("PENDING"));
        all.addAll(repo2030.findByProcessingStatus("PENDING"));
        return all;
    }
}
