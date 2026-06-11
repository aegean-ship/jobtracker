package dev.aegeanship.jobtracker.jobapplicationservice.interview.controller;

import tools.jackson.databind.json.JsonMapper;
import dev.aegeanship.jobtracker.jobapplicationservice.common.exception.InvalidStatusTransitionException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.response.InterviewResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.exception.InterviewNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterviewController.class)
class InterviewControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID INTERVIEW_ID = UUID.randomUUID();
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String BASE_URL = "/api/v1/interviews";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private InterviewService interviewService;

    @Test
    void createReturns201WithEnvelope() throws Exception {
        when(interviewService.create(eq(USER_ID), any(InterviewCreateRequest.class)))
                .thenReturn(interviewResponse(InterviewStatus.SCHEDULED, null));

        mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(INTERVIEW_ID.toString()))
                .andExpect(jsonPath("$.data.jobApplicationId").value(APPLICATION_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    void createWithMissingRequiredFieldsReturnsValidationErrors() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"round\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'jobApplicationId')]").exists())
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'type')]").exists());

        verify(interviewService, never()).create(any(), any());
    }

    @Test
    void getByIdReturnsInterview() throws Exception {
        when(interviewService.getById(USER_ID, INTERVIEW_ID))
                .thenReturn(interviewResponse(InterviewStatus.SCHEDULED, null));

        mockMvc.perform(get(BASE_URL + "/{id}", INTERVIEW_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(INTERVIEW_ID.toString()))
                .andExpect(jsonPath("$.data.type").value("TECHNICAL"));
    }

    @Test
    void getByIdReturns404EnvelopeWhenNotFound() throws Exception {
        when(interviewService.getById(USER_ID, INTERVIEW_ID))
                .thenThrow(new InterviewNotFoundException(INTERVIEW_ID.toString()));

        mockMvc.perform(get(BASE_URL + "/{id}", INTERVIEW_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INTERVIEW_NOT_FOUND"));
    }

    @Test
    void getAllByApplicationReturnsInterviews() throws Exception {
        when(interviewService.getAllByApplication(USER_ID, APPLICATION_ID))
                .thenReturn(List.of(interviewResponse(InterviewStatus.SCHEDULED, null)));

        mockMvc.perform(get(BASE_URL)
                        .param("jobApplicationId", APPLICATION_ID.toString())
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(INTERVIEW_ID.toString()));
    }

    @Test
    void getAllWithoutApplicationIdParamReturns400Envelope() throws Exception {
        mockMvc.perform(get(BASE_URL).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void updateStatusReturnsUpdatedInterview() throws Exception {
        when(interviewService.updateStatus(
                eq(USER_ID), eq(INTERVIEW_ID), any(InterviewStatusUpdateRequest.class)))
                .thenReturn(interviewResponse(InterviewStatus.COMPLETED,
                        "[SCHEDULED -> COMPLETED] went well"));

        mockMvc.perform(patch(BASE_URL + "/{id}/status", INTERVIEW_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new InterviewStatusUpdateRequest(
                                        InterviewStatus.COMPLETED, "went well"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.notes").value("[SCHEDULED -> COMPLETED] went well"));
    }

    @Test
    void updateStatusReturns409EnvelopeForInvalidTransition() throws Exception {
        when(interviewService.updateStatus(
                eq(USER_ID), eq(INTERVIEW_ID), any(InterviewStatusUpdateRequest.class)))
                .thenThrow(new InvalidStatusTransitionException("COMPLETED", "CANCELLED"));

        mockMvc.perform(patch(BASE_URL + "/{id}/status", INTERVIEW_ID)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(
                                new InterviewStatusUpdateRequest(InterviewStatus.CANCELLED, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void deleteReturns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", INTERVIEW_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(interviewService).delete(USER_ID, INTERVIEW_ID);
    }

    private InterviewCreateRequest createRequest() {
        return new InterviewCreateRequest(
                APPLICATION_ID, InterviewType.TECHNICAL, null, 1, 60, null, null, null);
    }

    private InterviewResponse interviewResponse(InterviewStatus status, String notes) {
        return new InterviewResponse(
                INTERVIEW_ID, APPLICATION_ID, InterviewType.TECHNICAL, status,
                1, null, 60, null, null, notes, Instant.now(), Instant.now());
    }
}
