package dev.aegeanship.jobtracker.jobapplicationservice.interview.controller;

import dev.aegeanship.jobtracker.common.controller.BaseController;
import dev.aegeanship.jobtracker.common.response.ApiStandardResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.response.InterviewResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for interviews. The acting user is taken from the
 * X-User-Id header until JWT authentication is in place; it will then come
 * from the token's subject claim instead.
 */
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController extends BaseController {

    static final String USER_ID_HEADER = "X-User-Id";

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<ApiStandardResponse<InterviewResponse>> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody InterviewCreateRequest request) {
        return created(interviewService.create(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiStandardResponse<InterviewResponse>> getById(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        return ok(interviewService.getById(userId, id));
    }

    @GetMapping
    public ResponseEntity<ApiStandardResponse<List<InterviewResponse>>> getAllByApplication(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestParam UUID jobApplicationId) {
        return ok(interviewService.getAllByApplication(userId, jobApplicationId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiStandardResponse<InterviewResponse>> updateStatus(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody InterviewStatusUpdateRequest request) {
        return ok(interviewService.updateStatus(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        interviewService.delete(userId, id);
        return noContent();
    }
}
