package dev.aegeanship.jobtracker.jobapplicationservice.application.controller;

import tools.jackson.databind.json.JsonMapper;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.ApplicationStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.ApplicationStatusHistoryResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.JobApplicationResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.application.service.JobApplicationService;
import dev.aegeanship.jobtracker.jobapplicationservice.common.config.WebConfig;
import dev.aegeanship.jobtracker.jobapplicationservice.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobApplicationController.class)
@Import(WebConfig.class)
class JobApplicationControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String BASE_URL = "/api/v1/applications";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    @Test
    void createReturns201WithEnvelope() throws Exception {
        when(jobApplicationService.create(eq(USER_ID), any(JobApplicationCreateRequest.class)))
                .thenReturn(applicationResponse(ApplicationStatus.APPLIED));

        mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(APPLICATION_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void createWithoutUserHeaderReturns400Envelope() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(jobApplicationService, never()).create(any(), any());
    }

    @Test
    void createWithInvalidBodyReturnsValidationFieldErrors() throws Exception {
        JobApplicationCreateRequest invalid = new JobApplicationCreateRequest(
                "  ", null, "Backend Engineer", null, null, null,
                new BigDecimal("-1"), null, null, null, null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'companyName')]").exists())
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'salaryMin')]").exists());

        verify(jobApplicationService, never()).create(any(), any());
    }

    @Test
    void createWithNonInitialStatusReturns400Envelope() throws Exception {
        // OFFER is a valid ApplicationStatus but not a valid InitialStatus
        mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Acme\",\"positionTitle\":\"Backend Engineer\","
                                + "\"status\":\"OFFER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                // the Jackson parse error (class names, accepted values) must stay server-side
                .andExpect(jsonPath("$.error.message")
                        .value("Request body is malformed or contains invalid values"));

        verify(jobApplicationService, never()).create(any(), any());
    }

    @Test
    void createSavedWithAppliedAtReturnsValidationError() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Acme\",\"positionTitle\":\"Backend Engineer\","
                                + "\"status\":\"SAVED\",\"appliedAt\":\"2026-06-12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verify(jobApplicationService, never()).create(any(), any());
    }

    @Test
    void getByIdReturnsApplication() throws Exception {
        when(jobApplicationService.getById(USER_ID, APPLICATION_ID))
                .thenReturn(applicationResponse(ApplicationStatus.APPLIED));

        mockMvc.perform(get(BASE_URL + "/{id}", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(APPLICATION_ID.toString()))
                .andExpect(jsonPath("$.data.companyName").value("Acme"));
    }

    @Test
    void getByIdReturns404EnvelopeWhenNotFound() throws Exception {
        when(jobApplicationService.getById(USER_ID, APPLICATION_ID))
                .thenThrow(new JobApplicationNotFoundException(APPLICATION_ID.toString()));

        mockMvc.perform(get(BASE_URL + "/{id}", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOB_APPLICATION_NOT_FOUND"));
    }

    @Test
    void getByIdWithMalformedUuidReturns400Envelope() throws Exception {
        mockMvc.perform(get(BASE_URL + "/not-a-uuid")
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.message")
                        .value("Parameter 'id' has an invalid value"));
    }

    @Test
    void getAllAppliesPageableDefaultsAndSerializesPagedModel() throws Exception {
        when(jobApplicationService.getAllByUser(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(applicationResponse(ApplicationStatus.APPLIED)),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE_URL).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(APPLICATION_ID.toString()))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobApplicationService).getAllByUser(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void updateReturnsUpdatedApplication() throws Exception {
        when(jobApplicationService.update(
                eq(USER_ID), eq(APPLICATION_ID), any(JobApplicationUpdateRequest.class)))
                .thenReturn(applicationResponse(ApplicationStatus.APPLIED));

        mockMvc.perform(put(BASE_URL + "/{id}", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new JobApplicationUpdateRequest(
                                "Acme", null, "Backend Engineer",
                                null, null, null, null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(APPLICATION_ID.toString()))
                .andExpect(jsonPath("$.data.companyName").value("Acme"));
    }

    @Test
    void updateWithInvalidBodyReturnsValidationFieldErrors() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'companyName')]").exists())
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'positionTitle')]").exists());

        verify(jobApplicationService, never()).update(any(), any(), any());
    }

    @Test
    void updateStatusReturnsUpdatedApplication() throws Exception {
        when(jobApplicationService.updateStatus(
                eq(USER_ID), eq(APPLICATION_ID), any(ApplicationStatusUpdateRequest.class)))
                .thenReturn(applicationResponse(ApplicationStatus.SCREENING));

        mockMvc.perform(patch(BASE_URL + "/{id}/status", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new ApplicationStatusUpdateRequest(
                                        ApplicationStatus.SCREENING, "recruiter call"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCREENING"));
    }

    @Test
    void updateStatusReturns409EnvelopeForInvalidTransition() throws Exception {
        when(jobApplicationService.updateStatus(
                eq(USER_ID), eq(APPLICATION_ID), any(ApplicationStatusUpdateRequest.class)))
                .thenThrow(new InvalidStatusTransitionException("ACCEPTED", "APPLIED"));

        mockMvc.perform(patch(BASE_URL + "/{id}/status", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new ApplicationStatusUpdateRequest(ApplicationStatus.APPLIED, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void updateStatusWithMissingToStatusReturnsValidationError() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/status", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"no target\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'toStatus')]").exists());
    }

    @Test
    void updateStatusWithUnknownEnumValueReturns400Envelope() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/status", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.message")
                        .value("Request body is malformed or contains invalid values"));
    }

    @Test
    void getStatusHistoryReturnsEntries() throws Exception {
        when(jobApplicationService.getStatusHistory(USER_ID, APPLICATION_ID))
                .thenReturn(List.of(new ApplicationStatusHistoryResponse(
                        UUID.randomUUID(), APPLICATION_ID, null,
                        ApplicationStatus.APPLIED, null, Instant.now())));

        mockMvc.perform(get(BASE_URL + "/{id}/history", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].toStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[0].jobApplicationId").value(APPLICATION_ID.toString()));
    }

    @Test
    void deleteReturns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", APPLICATION_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(jobApplicationService).delete(USER_ID, APPLICATION_ID);
    }

    private JobApplicationCreateRequest createRequest() {
        return new JobApplicationCreateRequest(
                "Acme", null, "Backend Engineer", null, null, null,
                null, null, null, null, null, null, null);
    }

    private JobApplicationResponse applicationResponse(ApplicationStatus status) {
        return new JobApplicationResponse(
                APPLICATION_ID, USER_ID, "Acme", null, "Backend Engineer",
                null, null, null, null, null, null, status, null, null, null,
                Instant.now(), Instant.now());
    }
}
