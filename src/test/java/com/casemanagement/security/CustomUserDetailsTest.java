package com.casemanagement.security;

import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomUserDetailsTest {

    @Test
    void exposesUserIdentityAndRoleAuthority() {
        UUID id = UUID.randomUUID();
        User user = new User("admin@example.com", "hashed-password", UserRole.ADMIN);
        ReflectionTestUtils.setField(user, "id", id);

        CustomUserDetails details = new CustomUserDetails(user);

        assertEquals(id, details.getId());
        assertEquals("admin@example.com", details.getUsername());
        assertEquals("hashed-password", details.getPassword());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertTrue(details.isEnabled());
        assertEquals(user, details.getUser());
    }
}
