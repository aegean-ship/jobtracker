package dev.aegeanship.jobtracker.jobapplicationservice.application.mapper;

import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.JobApplicationResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobApplicationMapper {

    // status is resolved in the service: request.status() if given,
    // otherwise the entity's @Builder.Default (APPLIED)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    JobApplication toEntity(JobApplicationCreateRequest request, UUID userId);

    // replaces the editable fields only; lifecycle fields stay untouched
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(JobApplicationUpdateRequest request,
                      @MappingTarget JobApplication application);

    JobApplicationResponse toResponse(JobApplication application);
}
