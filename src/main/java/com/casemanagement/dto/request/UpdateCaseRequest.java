package com.casemanagement.dto.request;

import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;

@Getter
public class UpdateCaseRequest {

    private String title;
    private String description;
    private CaseStatus status;
    private CasePriority priority;

    private boolean titleProvided;
    private boolean descriptionProvided;
    private boolean statusProvided;
    private boolean priorityProvided;

    @JsonSetter("title")
    public void setTitle(String title) {
        this.titleProvided = true;
        this.title = title;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.descriptionProvided = true;
        this.description = description;
    }

    @JsonSetter("status")
    public void setStatus(CaseStatus status) {
        this.statusProvided = true;
        this.status = status;
    }

    @JsonSetter("priority")
    public void setPriority(CasePriority priority) {
        this.priorityProvided = true;
        this.priority = priority;
    }
}