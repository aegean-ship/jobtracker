package dev.aegeanship.jobtracker.jobapplicationservice.application.controller;

import dev.aegeanship.jobtracker.common.controller.BaseController;
import dev.aegeanship.jobtracker.common.response.ApiStandardResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.ApplicationStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.ApplicationStatusHistoryResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.JobApplicationResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for job applications. The acting user is taken from the
 * X-User-Id header until JWT authentication is in place; it will then come
 * from the token's subject claim instead.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class JobApplicationController extends BaseController {

    static final String USER_ID_HEADER = "X-User-Id";

    private final JobApplicationService jobApplicationService;

    @PostMapping
    public ResponseEntity<ApiStandardResponse<JobApplicationResponse>> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody JobApplicationCreateRequest request) {
        return created(jobApplicationService.create(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiStandardResponse<JobApplicationResponse>> getById(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        return ok(jobApplicationService.getById(userId, id));
    }

    @GetMapping
    public ResponseEntity<ApiStandardResponse<Page<JobApplicationResponse>>> getAll(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ok(jobApplicationService.getAllByUser(userId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiStandardResponse<JobApplicationResponse>> update(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody JobApplicationUpdateRequest request) {
        return ok(jobApplicationService.update(userId, id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiStandardResponse<JobApplicationResponse>> updateStatus(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ok(jobApplicationService.updateStatus(userId, id, request));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiStandardResponse<List<ApplicationStatusHistoryResponse>>> getStatusHistory(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        return ok(jobApplicationService.getStatusHistory(userId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable UUID id) {
        jobApplicationService.delete(userId, id);
        return noContent();
    }
}
