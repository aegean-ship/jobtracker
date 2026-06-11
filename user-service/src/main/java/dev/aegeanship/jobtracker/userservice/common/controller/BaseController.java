package dev.aegeanship.jobtracker.userservice.common.controller;

import dev.aegeanship.jobtracker.userservice.common.response.ApiStandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

    protected <T> ResponseEntity<ApiStandardResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiStandardResponse.data(data));
    }

    protected <T> ResponseEntity<ApiStandardResponse<T>> created(T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiStandardResponse.data(data));
    }

    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

}
