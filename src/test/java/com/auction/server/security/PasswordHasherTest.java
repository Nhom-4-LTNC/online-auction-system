package com.auction.server.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hash_shouldNotEqualRawPassword() {
        String rawPassword = "StrongPassword123!";

        String hash = PasswordHasher.hash(rawPassword);

        assertNotEquals(rawPassword, hash);
        assertTrue(PasswordHasher.isBcryptHash(hash));
    }

    @Test
    void matches_correctPassword_shouldReturnTrue() {
        String rawPassword = "StrongPassword123!";
        String hash = PasswordHasher.hash(rawPassword);

        assertTrue(PasswordHasher.matches(rawPassword, hash));
    }

    @Test
    void matches_wrongPassword_shouldReturnFalse() {
        String hash = PasswordHasher.hash("StrongPassword123!");

        assertFalse(PasswordHasher.matches("WrongPassword123!", hash));
    }

    @Test
    void hash_samePasswordTwice_shouldProduceDifferentHashes() {
        String rawPassword = "StrongPassword123!";

        String firstHash = PasswordHasher.hash(rawPassword);
        String secondHash = PasswordHasher.hash(rawPassword);

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordHasher.matches(rawPassword, firstHash));
        assertTrue(PasswordHasher.matches(rawPassword, secondHash));
    }
}
