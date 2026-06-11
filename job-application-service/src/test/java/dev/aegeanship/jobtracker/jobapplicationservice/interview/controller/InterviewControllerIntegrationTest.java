package dev.aegeanship.jobtracker.jobapplicationservice.interview.controller;

import com.jayway.jsonpath.JsonPath;
import dev.aegeanship.jobtracker.jobapplicationservice.AbstractIntegrationTest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String APPLICATIONS_URL = "/api/v1/applications";
    private static final String INTERVIEWS_URL = "/api/v1/interviews";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID applicationId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("TRUNCATE TABLE job_applications CASCADE");
        userId = UUID.randomUUID();
        applicationId = createApplication(userId);
    }

    @Test
    void createFetchAndListRoundTrip() throws Exception {
        UUID interviewId = createInterview(userId, applicationId);

        mockMvc.perform(get(INTERVIEWS_URL + "/{id}", interviewId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(interviewId.toString()))
                .andExpect(jsonPath("$.data.jobApplicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        mockMvc.perform(get(INTERVIEWS_URL)
                        .param("jobApplicationId", applicationId.toString())
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(interviewId.toString()));
    }

    @Test
    void createForForeignApplicationReturns404() throws Exception {
        mockMvc.perform(post(INTERVIEWS_URL)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new InterviewCreateRequest(
                                applicationId, InterviewType.TECHNICAL,
                                null, 1, 60, null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOB_APPLICATION_NOT_FOUND"));
    }

    @Test
    void statusUpdateStampsNotesAndTerminalStateRejectsFurtherUpdates() throws Exception {
        UUID interviewId = createInterview(userId, applicationId);

        mockMvc.perform(patch(INTERVIEWS_URL + "/{id}/status", interviewId)
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new InterviewStatusUpdateRequest(
                                InterviewStatus.COMPLETED, "went well"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.notes").value("[SCHEDULED -> COMPLETED] went well"));

        mockMvc.perform(patch(INTERVIEWS_URL + "/{id}/status", interviewId)
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new InterviewStatusUpdateRequest(
                                InterviewStatus.CANCELLED, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void interviewsDisappearWhenApplicationIsSoftDeleted() throws Exception {
        UUID interviewId = createInterview(userId, applicationId);

        mockMvc.perform(delete(APPLICATIONS_URL + "/{id}", applicationId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(INTERVIEWS_URL + "/{id}", interviewId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INTERVIEW_NOT_FOUND"));

        mockMvc.perform(get(INTERVIEWS_URL)
                        .param("jobApplicationId", applicationId.toString())
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOB_APPLICATION_NOT_FOUND"));
    }

    @Test
    void deleteRemovesInterviewButKeepsApplication() throws Exception {
        UUID interviewId = createInterview(userId, applicationId);

        mockMvc.perform(delete(INTERVIEWS_URL + "/{id}", interviewId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(INTERVIEWS_URL)
                        .param("jobApplicationId", applicationId.toString())
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get(APPLICATIONS_URL + "/{id}", applicationId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk());
    }

    private UUID createApplication(UUID ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post(APPLICATIONS_URL)
                        .header(USER_ID_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new JobApplicationCreateRequest(
                                "Acme", null, "Backend Engineer", null, null, null,
                                null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                JsonPath.read(result.getResponse().getContentAsString(), "$.data.id"));
    }

    private UUID createInterview(UUID ownerId, UUID forApplicationId) throws Exception {
        MvcResult result = mockMvc.perform(post(INTERVIEWS_URL)
                        .header(USER_ID_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new InterviewCreateRequest(
                                forApplicationId, InterviewType.TECHNICAL,
                                null, 1, 60, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                JsonPath.read(result.getResponse().getContentAsString(), "$.data.id"));
    }
}
