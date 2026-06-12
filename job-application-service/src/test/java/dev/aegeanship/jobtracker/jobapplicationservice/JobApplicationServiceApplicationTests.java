package dev.aegeanship.jobtracker.jobapplicationservice;

import com.jayway.jsonpath.JsonPath;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.ApplicationStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.WorkMode;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * End-to-end test against a real servlet container: the application is
 * started on a random port and exercised over actual HTTP with
 * {@link RestTestClient}, backed by the shared Testcontainers Postgres.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobApplicationServiceApplicationTests extends AbstractIntegrationTest {

	private static final String USER_ID_HEADER = "X-User-Id";
	private static final String APPLICATIONS_URL = "/api/v1/applications";
	private static final String INTERVIEWS_URL = "/api/v1/interviews";

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private RestTestClient client;
	private UUID userId;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE TABLE job_applications CASCADE");
		userId = UUID.randomUUID();
		client = RestTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();
	}

	/**
	 * Full lifecycle of one job hunt: apply, progress through interviews,
	 * receive and accept an offer, then delete the application and verify
	 * everything is gone from the API.
	 */
	@Test
	void endToEndJobApplicationLifecycle() {
		// apply at Acme
		byte[] created = client.post().uri(APPLICATIONS_URL)
				.header(USER_ID_HEADER, userId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new JobApplicationCreateRequest(
						"Acme", "https://acme.dev", "Backend Engineer",
						"https://acme.dev/jobs/42", "Izmir", WorkMode.HYBRID,
						new BigDecimal("90000"), new BigDecimal("120000"), "EUR",
						LocalDate.now(), "LinkedIn", "referred by a friend", null))
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.data.status").isEqualTo("APPLIED")
				.returnResult().getResponseBody();
		UUID applicationId = idFrom(created);

		// it shows up in the list
		client.get().uri(APPLICATIONS_URL)
				.header(USER_ID_HEADER, userId.toString())
				.exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.data.page.totalElements").isEqualTo(1);

		// recruiter reaches out: move to INTERVIEWING
		updateApplicationStatus(applicationId, ApplicationStatus.INTERVIEWING, "phone screen booked");

		// schedule and complete the technical interview
		byte[] interviewCreated = client.post().uri(INTERVIEWS_URL)
				.header(USER_ID_HEADER, userId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new InterviewCreateRequest(
						applicationId, InterviewType.TECHNICAL, null,
						1, 60, "Jane Doe", "https://meet.acme.dev/42", null))
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.data.status").isEqualTo("SCHEDULED")
				.returnResult().getResponseBody();
		UUID interviewId = idFrom(interviewCreated);

		client.patch().uri(INTERVIEWS_URL + "/{id}/status", interviewId)
				.header(USER_ID_HEADER, userId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new InterviewStatusUpdateRequest(InterviewStatus.COMPLETED, "strong loop"))
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.notes").isEqualTo("[SCHEDULED -> COMPLETED] strong loop");

		// offer arrives and is accepted
		updateApplicationStatus(applicationId, ApplicationStatus.OFFER, "offer received");
		updateApplicationStatus(applicationId, ApplicationStatus.ACCEPTED, "signed!");

		// the history tells the whole story in order
		client.get().uri(APPLICATIONS_URL + "/{id}/history", applicationId)
				.header(USER_ID_HEADER, userId.toString())
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.length()").isEqualTo(4)
				.jsonPath("$.data[0].toStatus").isEqualTo("APPLIED")
				.jsonPath("$.data[1].toStatus").isEqualTo("INTERVIEWING")
				.jsonPath("$.data[2].toStatus").isEqualTo("OFFER")
				.jsonPath("$.data[3].toStatus").isEqualTo("ACCEPTED");

		// terminal state: no further transitions allowed
		client.patch().uri(APPLICATIONS_URL + "/{id}/status", applicationId)
				.header(USER_ID_HEADER, userId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ApplicationStatusUpdateRequest(ApplicationStatus.WITHDRAWN, null))
				.exchange()
				.expectStatus().isEqualTo(409)
				.expectBody().jsonPath("$.error.code").isEqualTo("INVALID_STATUS_TRANSITION");

		// clean up: delete the application, everything disappears
		client.delete().uri(APPLICATIONS_URL + "/{id}", applicationId)
				.header(USER_ID_HEADER, userId.toString())
				.exchange()
				.expectStatus().isNoContent();

		client.get().uri(APPLICATIONS_URL + "/{id}", applicationId)
				.header(USER_ID_HEADER, userId.toString())
				.exchange()
				.expectStatus().isNotFound();
		client.get().uri(INTERVIEWS_URL + "/{id}", interviewId)
				.header(USER_ID_HEADER, userId.toString())
				.exchange()
				.expectStatus().isNotFound();
		client.get().uri(APPLICATIONS_URL)
				.header(USER_ID_HEADER, userId.toString())
				.exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.data.page.totalElements").isEqualTo(0);
	}

	private void updateApplicationStatus(UUID applicationId, ApplicationStatus toStatus,
										 String note) {
		client.patch().uri(APPLICATIONS_URL + "/{id}/status", applicationId)
				.header(USER_ID_HEADER, userId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ApplicationStatusUpdateRequest(toStatus, note))
				.exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.data.status").isEqualTo(toStatus.name());
	}

	private UUID idFrom(byte[] responseBody) {
		return UUID.fromString(JsonPath.read(new String(responseBody, java.nio.charset.StandardCharsets.UTF_8), "$.data.id"));
	}
}
