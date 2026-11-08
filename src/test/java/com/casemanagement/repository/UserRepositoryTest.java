package com.casemanagement.repository;

import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUserByEmailAndChecksEmailExistence() {
        String email = uniqueEmail();
        userRepository.saveAndFlush(new User(email, "hashed-password", UserRole.AGENT));

        assertTrue(userRepository.existsByEmail(email));
        assertEquals(email, userRepository.findByEmail(email).orElseThrow().getEmail());
        assertFalse(userRepository.existsByEmail(uniqueEmail()));
        assertTrue(userRepository.findByEmail(uniqueEmail()).isEmpty());
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
