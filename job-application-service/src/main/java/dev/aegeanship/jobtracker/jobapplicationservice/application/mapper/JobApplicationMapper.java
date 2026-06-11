package dev.aegeanship.jobtracker.jobapplicationservice.application.mapper;

import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.JobApplicationResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobApplicationMapper {

    // status is ignored so the entity's @Builder.Default (APPLIED) applies
    @Mapping(target = "status", ignore = true)
    JobApplication toEntity(JobApplicationCreateRequest request, UUID userId);

    JobApplicationResponse toResponse(JobApplication application);
}
