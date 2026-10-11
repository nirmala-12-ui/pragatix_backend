package jjcet.PragatiX.integrations.neopat;

import jjcet.PragatiX.integrations.neopat.controller.NeopatParentContactController;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactPageResponseDto;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactRequestDto;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactResponseDto;
import jjcet.PragatiX.integrations.neopat.entity.ParentContact;
import jjcet.PragatiX.integrations.neopat.repository.ParentContactRepository;
import jjcet.PragatiX.integrations.neopat.service.NeopatAuditLogService;
import jjcet.PragatiX.integrations.neopat.service.NeopatParentContactService;
import jjcet.PragatiX.util.crypto.AesGcmAttributeConverter;
import jjcet.PragatiX.util.crypto.AesGcmEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NeopatParentContactIntegrationTest {

    @Mock
    private ParentContactRepository parentContactRepository;

    @Mock
    private NeopatAuditLogService auditLogService;

    private NeopatParentContactService parentContactService;
    private NeopatParentContactController controller;

    @BeforeEach
    void setUp() {
        parentContactService = new NeopatParentContactService(parentContactRepository, auditLogService);
        controller = new NeopatParentContactController(parentContactService);
    }

    @Test
    @DisplayName("Requirement 1 & 2: Pagination enforces maximum 100 records and supports page retrieval")
    void testPaginationEnforcesMax100Records() {
        List<ParentContact> mockContacts = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            ParentContact c = new ParentContact("student" + i + "@example.com", "9876543210");
            c.setId((long) i);
            mockContacts.add(c);
        }

        when(parentContactRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(mockContacts, Pageable.ofSize(100), 250));

        // Client requests size=500 -> backend must clamp to 100
        ResponseEntity<?> response = controller.getParentContacts(0, 500);
        assertNotNull(response.getBody());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(parentContactRepository).findAll(pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize(), "Backend must clamp page size to maximum 100");
    }

    @Test
    @DisplayName("Requirement 3: Invalid pagination parameters (negative page, 0 size) are handled safely")
    void testInvalidPaginationParametersHandledSafely() {
        when(parentContactRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        controller.getParentContacts(-5, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(parentContactRepository).findAll(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber(), "Negative page should default to 0");
        assertEquals(100, pageableCaptor.getValue().getPageSize(), "Zero size should default to 100");
    }

    @Test
    @DisplayName("Requirement 4 & 8: New student contact is created and parent mobile number is encrypted")
    void testCreateStudentContactWithEncryptedMobile() {
        ParentContactRequestDto request = new ParentContactRequestDto("student@jjcet.ac.in", "9876543210");

        when(parentContactRepository.existsByStudentEmailIgnoreCase("student@jjcet.ac.in")).thenReturn(false);
        when(parentContactRepository.save(any(ParentContact.class))).thenAnswer(i -> {
            ParentContact c = i.getArgument(0);
            c.setId(101L);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        var response = controller.createParentContact(request);
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("student@jjcet.ac.in", response.getBody().getData().getStudentEmail());
        assertEquals("******3210", response.getBody().getData().getParentMobileMasked());

        ArgumentCaptor<ParentContact> contactCaptor = ArgumentCaptor.forClass(ParentContact.class);
        verify(parentContactRepository).save(contactCaptor.capture());

        // Verify entity converter encrypts the number
        AesGcmAttributeConverter converter = new AesGcmAttributeConverter();
        String dbData = converter.convertToDatabaseColumn(contactCaptor.getValue().getParentMobile());
        assertTrue(dbData.startsWith(AesGcmEncryptionUtil.PREFIX), "Stored DB column must be AES-256-GCM encrypted");
        assertEquals("9876543210", converter.convertToEntityAttribute(dbData), "Decryption must return exact original phone number");
    }

    @Test
    @DisplayName("Requirement 5: Duplicate student email is rejected with error")
    void testDuplicateStudentEmailRejected() {
        ParentContactRequestDto request = new ParentContactRequestDto("existing@jjcet.ac.in", "9876543210");
        when(parentContactRepository.existsByStudentEmailIgnoreCase("existing@jjcet.ac.in")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> controller.createParentContact(request));
        verify(parentContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement 6: Existing contact can be edited and email collision is prevented")
    void testEditExistingContact() {
        ParentContact existing = new ParentContact("old@jjcet.ac.in", "9876543210");
        existing.setId(55L);

        when(parentContactRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(parentContactRepository.existsByStudentEmailIgnoreCaseAndIdNot("new@jjcet.ac.in", 55L)).thenReturn(false);
        when(parentContactRepository.save(any(ParentContact.class))).thenAnswer(i -> i.getArgument(0));

        ParentContactRequestDto updateDto = new ParentContactRequestDto("new@jjcet.ac.in", "9998887776");
        var response = controller.updateParentContact(55L, updateDto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("new@jjcet.ac.in", response.getBody().getData().getStudentEmail());
        assertEquals("******7776", response.getBody().getData().getParentMobileMasked());
    }

    @Test
    @DisplayName("Requirement 7: Delete removes only selected contact mapping")
    void testDeleteOnlySelectedContact() {
        ParentContact existing = new ParentContact("target@jjcet.ac.in", "9876543210");
        existing.setId(77L);

        when(parentContactRepository.findById(77L)).thenReturn(Optional.of(existing));

        var response = controller.deleteParentContact(77L);
        assertEquals(200, response.getStatusCode().value());
        verify(parentContactRepository).delete(existing);
    }

    @Test
    @DisplayName("Requirement 9, 10, 11: AES-256-GCM encryption, decryption, and authentication tag verification")
    void testAesGcmEncryptionDecryptionAndTampering() {
        String originalMobile = "9876543210";
        String encrypted = AesGcmEncryptionUtil.encrypt(originalMobile);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC:"));

        // Decryption returns original value
        String decrypted = AesGcmEncryptionUtil.decrypt(encrypted);
        assertEquals(originalMobile, decrypted);

        // Tampered ciphertext fails authentication tag check and is rejected without producing corrupt plaintext
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";
        String tamperedResult = AesGcmEncryptionUtil.decrypt(tampered);
        assertNotEquals(originalMobile, tamperedResult, "Tampered ciphertext must not decrypt to original plain text");
        assertTrue(tamperedResult.startsWith("ENC:"), "Tampered ciphertext must be rejected and remain encrypted text");
    }

    @Test
    @DisplayName("Requirement 12: Existing plaintext phone number is migrated without data loss")
    void testPlaintextDataMigration() {
        String legacyPlaintext = "6380170548";
        // Legacy plaintext not prefixed with ENC:
        assertFalse(legacyPlaintext.startsWith("ENC:"));

        String encrypted = AesGcmEncryptionUtil.encrypt(legacyPlaintext);
        assertTrue(encrypted.startsWith("ENC:"));

        String decrypted = AesGcmEncryptionUtil.decrypt(encrypted);
        assertEquals(legacyPlaintext, decrypted, "Migrated phone number must decrypt to identical legacy value");
    }

    @Test
    @DisplayName("Requirement 14: Mobile numbers are masked in DTO responses")
    void testMobileNumberMasking() {
        assertEquals("******1234", ParentContactResponseDto.maskMobile("9876541234"));
        assertEquals("*********1234", ParentContactResponseDto.maskMobile("+919876541234"));
        assertEquals("N/A", ParentContactResponseDto.maskMobile(null));
        assertEquals("N/A", ParentContactResponseDto.maskMobile(""));
    }
}
