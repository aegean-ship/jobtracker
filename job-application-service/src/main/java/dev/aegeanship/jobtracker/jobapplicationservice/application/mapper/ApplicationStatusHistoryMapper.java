package dev.aegeanship.jobtracker.jobapplicationservice.application.mapper;

import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.ApplicationStatusHistoryResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.ApplicationStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationStatusHistoryMapper {

    @Mapping(target = "jobApplicationId", source = "jobApplication.id")
    ApplicationStatusHistoryResponse toResponse(ApplicationStatusHistory history);
}
