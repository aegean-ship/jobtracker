package dev.aegeanship.jobtracker.jobapplicationservice.interview.mapper;

import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.response.InterviewResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.entity.Interview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InterviewMapper {

    // status is ignored so the entity's @Builder.Default (SCHEDULED) applies
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "notes", source = "request.notes")
    Interview toEntity(InterviewCreateRequest request, JobApplication jobApplication);

    // replaces the editable fields only; parent and status stay untouched
    @Mapping(target = "jobApplication", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(InterviewUpdateRequest request, @MappingTarget Interview interview);

    @Mapping(target = "jobApplicationId", source = "jobApplication.id")
    InterviewResponse toResponse(Interview interview);
}
