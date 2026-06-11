package dev.aegeanship.jobtracker.jobapplicationservice.application.controller;

import com.jayway.jsonpath.JsonPath;
import dev.aegeanship.jobtracker.jobapplicationservice.AbstractIntegrationTest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.ApplicationStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
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

class JobApplicationControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String BASE_URL = "/api/v1/applications";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE job_applications CASCADE");
        userId = UUID.randomUUID();
    }

    @Test
    void createAndFetchRoundTrip() throws Exception {
        UUID applicationId = createApplication(userId, "Acme");

        mockMvc.perform(get(BASE_URL + "/{id}", applicationId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.data.companyName").value("Acme"))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void applicationsAreHiddenFromOtherUsers() throws Exception {
        UUID applicationId = createApplication(userId, "Acme");

        mockMvc.perform(get(BASE_URL + "/{id}", applicationId)
                        .header(USER_ID_HEADER, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOB_APPLICATION_NOT_FOUND"));
    }

    @Test
    void statusUpdatePersistsAndHistoryIsExposed() throws Exception {
        UUID applicationId = createApplication(userId, "Acme");

        mockMvc.perform(patch(BASE_URL + "/{id}/status", applicationId)
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdate(ApplicationStatus.SCREENING, "recruiter call")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCREENING"));

        mockMvc.perform(get(BASE_URL + "/{id}/history", applicationId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].toStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[1].fromStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[1].toStatus").value("SCREENING"))
                .andExpect(jsonPath("$.data[1].note").value("recruiter call"));
    }

    @Test
    void invalidTransitionReturns409ThroughFullStack() throws Exception {
        UUID applicationId = createApplication(userId, "Acme");

        mockMvc.perform(patch(BASE_URL + "/{id}/status", applicationId)
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdate(ApplicationStatus.ACCEPTED, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void listPaginatesOverPersistedData() throws Exception {
        createApplication(userId, "Acme");
        createApplication(userId, "Globex");
        createApplication(userId, "Initech");
        createApplication(UUID.randomUUID(), "SomeoneElsesCorp");

        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "2")
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(3))
                .andExpect(jsonPath("$.data.page.totalPages").value(2));
    }

    @Test
    void deleteHidesApplicationFromSubsequentRequests() throws Exception {
        UUID applicationId = createApplication(userId, "Acme");

        mockMvc.perform(delete(BASE_URL + "/{id}", applicationId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/{id}", applicationId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOB_APPLICATION_NOT_FOUND"));
    }

    @Test
    void validationFailuresReturnFieldErrorsThroughFullStack() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new JobApplicationCreateRequest(
                                "  ", null, "Backend Engineer", null, null, null,
                                null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'companyName')]").exists());
    }

    private UUID createApplication(UUID ownerId, String companyName) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header(USER_ID_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new JobApplicationCreateRequest(
                                companyName, null, "Backend Engineer", null, null, null,
                                null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                JsonPath.read(result.getResponse().getContentAsString(), "$.data.id"));
    }

    private String statusUpdate(ApplicationStatus toStatus, String note) {
        return jsonMapper.writeValueAsString(new ApplicationStatusUpdateRequest(toStatus, note));
    }
}
