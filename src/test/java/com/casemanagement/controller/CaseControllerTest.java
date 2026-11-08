package com.casemanagement.controller;

import com.casemanagement.dto.request.CaseFilterRequest;
import com.casemanagement.dto.request.CreateCaseRequest;
import com.casemanagement.dto.request.UpdateCaseRequest;
import com.casemanagement.dto.response.CaseResponse;
import com.casemanagement.security.AuthenticatedUser;
import com.casemanagement.service.CaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseControllerTest {

    @Mock
    private CaseService caseService;

    @Mock
    private AuthenticatedUser currentUser;

    @InjectMocks
    private CaseController controller;

    @Test
    void createCaseReturnsCreatedLocationAndUsesAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        CaseResponse body = CaseResponse.builder().id(caseId).build();
        CreateCaseRequest request = new CreateCaseRequest();
        when(currentUser.getId()).thenReturn(userId);
        when(caseService.createCase(request, userId, "key-1")).thenReturn(body);

        var response = controller.createCase(request, "key-1", currentUser);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/cases/" + caseId, response.getHeaders().getLocation().toString());
        assertEquals(body, response.getBody());
        verify(caseService).createCase(request, userId, "key-1");
    }

    @Test
    void getCasesBuildsRequestedPageAndSort() {
        Page<CaseResponse> page = new PageImpl<>(List.of());
        when(caseService.getCases(any(CaseFilterRequest.class), any())).thenReturn(page);

        var response = controller.getCases(new CaseFilterRequest(), 2, 15, "title", "ASC");

        assertEquals(page, response.getBody());
        verify(caseService).getCases(any(CaseFilterRequest.class),
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.getPageNumber() == 2
                                && request.getPageSize() == 15
                                && request.getSort().getOrderFor("title").getDirection() == Sort.Direction.ASC));
    }

    @Test
    void getCasesRejectsInvalidPageSizeAndSortWithoutCallingService() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getCases(new CaseFilterRequest(), 0, 101, "title", "ASC"));
        assertThrows(IllegalArgumentException.class,
                () -> controller.getCases(new CaseFilterRequest(), 0, 10, "password", "ASC"));
        assertThrows(IllegalArgumentException.class,
                () -> controller.getCases(new CaseFilterRequest(), 0, 10, "title", "SIDEWAYS"));

        verify(caseService, never()).getCases(any(), any());
    }

    @Test
    void deleteCaseReturnsNoContentAndPassesAuthenticatedUser() {
        UUID caseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(currentUser.getId()).thenReturn(userId);

        var response = controller.deleteCase(caseId, currentUser);

        assertEquals(204, response.getStatusCode().value());
        verify(caseService).deleteCase(caseId, userId);
    }
}
