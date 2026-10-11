package jjcet.PragatiX.integrations.neopat.service;

import jjcet.PragatiX.common.exception.ResourceNotFoundException;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactPageResponseDto;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactRequestDto;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactResponseDto;
import jjcet.PragatiX.integrations.neopat.entity.ParentContact;
import jjcet.PragatiX.integrations.neopat.repository.ParentContactRepository;
import jjcet.PragatiX.util.crypto.AesGcmEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NeopatParentContactService {

    private static final Logger log = LoggerFactory.getLogger(NeopatParentContactService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ParentContactRepository parentContactRepository;
    private final NeopatAuditLogService auditLogService;

    public NeopatParentContactService(ParentContactRepository parentContactRepository,
                                     NeopatAuditLogService auditLogService) {
        this.parentContactRepository = parentContactRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Server-side paginated retrieval of parent contact mappings.
     * Enforces a hard maximum limit of 100 records per page.
     */
    @Transactional(readOnly = true, transactionManager = "neopatTransactionManager")
    public ParentContactPageResponseDto getPaginatedContacts(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0 || size > MAX_PAGE_SIZE) ? MAX_PAGE_SIZE : size;

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<ParentContact> contactPage = parentContactRepository.findAll(pageable);

        List<ParentContactResponseDto> dtos = contactPage.getContent().stream()
                .map(this::mapToResponseDto)
                .toList();

        return new ParentContactPageResponseDto(
                dtos,
                contactPage.getNumber(),
                contactPage.getSize(),
                contactPage.getTotalElements(),
                contactPage.getTotalPages(),
                contactPage.isFirst(),
                contactPage.isLast()
        );
    }

    /**
     * Retrieve a single contact mapping by ID.
     */
    @Transactional(readOnly = true, transactionManager = "neopatTransactionManager")
    public ParentContactResponseDto getContactById(Long id) {
        ParentContact contact = parentContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student contact mapping not found with id: " + id));
        return mapToResponseDto(contact);
    }

    /**
     * Create a new student-parent contact mapping.
     * Normalizes inputs, enforces uniqueness on student email,
     * and persists with AES-256-GCM encryption on parent mobile.
     */
    @Transactional("neopatTransactionManager")
    public ParentContactResponseDto createContact(ParentContactRequestDto dto) {
        String normalizedEmail = normalizeEmail(dto.getStudentEmail());
        String normalizedMobile = normalizeMobile(dto.getParentMobile());

        if (parentContactRepository.existsByStudentEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Student contact mapping already exists for email: " + normalizedEmail);
        }

        ParentContact contact = new ParentContact(normalizedEmail, normalizedMobile);
        if (dto.getIsActive() != null) {
            contact.setIsActive(dto.getIsActive());
        }

        ParentContact saved = parentContactRepository.save(contact);

        auditLogService.logEvent(
                "PARENT_CONTACT",
                String.valueOf(saved.getId()),
                "CONTACT_CREATED",
                "SUCCESS",
                "Created parent contact mapping for " + normalizedEmail,
                null
        );

        return mapToResponseDto(saved);
    }

    /**
     * Update an existing contact mapping.
     */
    @Transactional("neopatTransactionManager")
    public ParentContactResponseDto updateContact(Long id, ParentContactRequestDto dto) {
        ParentContact contact = parentContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student contact mapping not found with id: " + id));

        String normalizedEmail = normalizeEmail(dto.getStudentEmail());

        // Check if updating email conflicts with another student's record
        if (parentContactRepository.existsByStudentEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new IllegalArgumentException("Another student contact already exists with email: " + normalizedEmail);
        }

        contact.setStudentEmail(normalizedEmail);

        // Only update phone number if provided and non-empty
        if (dto.getParentMobile() != null && !dto.getParentMobile().trim().isEmpty()) {
            String normalizedMobile = normalizeMobile(dto.getParentMobile());
            contact.setParentMobile(normalizedMobile);
        }

        if (dto.getIsActive() != null) {
            contact.setIsActive(dto.getIsActive());
        }

        ParentContact updated = parentContactRepository.save(contact);

        auditLogService.logEvent(
                "PARENT_CONTACT",
                String.valueOf(updated.getId()),
                "CONTACT_UPDATED",
                "SUCCESS",
                "Updated parent contact mapping for " + normalizedEmail,
                null
        );

        return mapToResponseDto(updated);
    }

    /**
     * Delete an existing contact mapping.
     * Only deletes from the neopa_sms.parent_contact table.
     * Never touches core PragatiX students or assessment history.
     */
    @Transactional("neopatTransactionManager")
    public void deleteContact(Long id) {
        ParentContact contact = parentContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student contact mapping not found with id: " + id));

        String studentEmail = contact.getStudentEmail();
        parentContactRepository.delete(contact);

        auditLogService.logEvent(
                "PARENT_CONTACT",
                String.valueOf(id),
                "CONTACT_DELETED",
                "SUCCESS",
                "Deleted parent contact mapping for student email: " + studentEmail,
                null
        );
    }

    private ParentContactResponseDto mapToResponseDto(ParentContact entity) {
        // entity.getParentMobile() is automatically decrypted by AesGcmAttributeConverter
        String plainOrDecrypted = entity.getParentMobile();
        if (plainOrDecrypted != null && plainOrDecrypted.startsWith(AesGcmEncryptionUtil.PREFIX)) {
            plainOrDecrypted = AesGcmEncryptionUtil.decrypt(plainOrDecrypted);
        }
        String masked = ParentContactResponseDto.maskMobile(plainOrDecrypted);

        return new ParentContactResponseDto(
                entity.getId(),
                entity.getStudentEmail(),
                masked,
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Student email must not be null");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) {
            throw new IllegalArgumentException("Parent mobile number must not be null");
        }
        return mobile.replaceAll("[\\s\\-()]", "");
    }
}
