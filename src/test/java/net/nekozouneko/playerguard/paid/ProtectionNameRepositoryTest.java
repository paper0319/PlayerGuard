package net.nekozouneko.playerguard.paid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectionNameRepositoryTest {
    private final ProtectionNameRepository repository = new ProtectionNameRepository();

    @Test
    void acceptsPlainNamesBetweenOneAndThirtyTwoCharacters() {
        assertEquals(ProtectionNameRepository.ValidationResult.OK, repository.validate("拠点A"));
    }

    @Test
    void rejectsTooShortOrTooLongNames() {
        assertEquals(ProtectionNameRepository.ValidationResult.OK, repository.validate("A"));
        assertEquals(ProtectionNameRepository.ValidationResult.INVALID_LENGTH, repository.validate("123456789012345678901234567890123"));
    }

    @Test
    void rejectsColorCodesAndLineBreaks() {
        assertEquals(ProtectionNameRepository.ValidationResult.COLOR_CODE, repository.validate("&a拠点"));
        assertEquals(ProtectionNameRepository.ValidationResult.COLOR_CODE, repository.validate("§a拠点"));
        assertEquals(ProtectionNameRepository.ValidationResult.LINE_BREAK, repository.validate("拠点\nA"));
    }
}