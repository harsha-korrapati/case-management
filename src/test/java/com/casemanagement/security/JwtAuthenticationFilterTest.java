package com.casemanagement.security;

import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingBearerHeaderContinuesWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void validTokenWithMatchingSubjectAuthenticatesLoadedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User("person@example.com", "hashed", UserRole.AGENT);
        ReflectionTestUtils.setField(user, "id", userId);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(jwtService.extractUserId("token")).thenReturn(userId);
        when(jwtService.extractEmail("token")).thenReturn(user.getEmail());
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validTokenWithDifferentSubjectDoesNotAuthenticate() throws Exception {
        UUID tokenUserId = UUID.randomUUID();
        User user = new User("person@example.com", "hashed", UserRole.AGENT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(jwtService.extractUserId("token")).thenReturn(tokenUserId);
        when(jwtService.extractEmail("token")).thenReturn(user.getEmail());
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(new CustomUserDetails(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
