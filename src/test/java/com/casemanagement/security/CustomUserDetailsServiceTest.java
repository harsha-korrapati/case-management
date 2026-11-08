package com.casemanagement.security;

import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import com.casemanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsernameReturnsUserDetailsForEmail() {
        User user = new User("agent@example.com", "hashed", UserRole.AGENT);
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(user.getEmail());

        assertEquals(user.getEmail(), details.getUsername());
        assertEquals("hashed", details.getPassword());
        assertEquals("ROLE_AGENT", details.getAuthorities().iterator().next().getAuthority());
        verify(repository).findByEmail(user.getEmail());
    }

    @Test
    void loadUserByUsernameThrowsWhenEmailDoesNotExist() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@example.com"));
    }
}
