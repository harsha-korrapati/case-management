package com.casemanagement.event;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class CorrelationIdProvider {

    public String getCorrelationId() {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return UUID.randomUUID().toString();
        }

        HttpServletRequest request = attributes.getRequest();

        Object correlationId = request.getAttribute("correlationId");

        if (correlationId instanceof String value && !value.isBlank()) {
            return value;
        }

        return UUID.randomUUID().toString();
    }
}
