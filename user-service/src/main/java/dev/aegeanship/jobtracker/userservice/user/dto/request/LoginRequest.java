package dev.aegeanship.jobtracker.userservice.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Identifier is required")
        @Size(min = 3, max = 255 , message = "Identifier must be between 3 and 255 characters")
        String identifier  ,

        @NotBlank(message = "Password is required")
        @Size(min = 10, max = 255 , message = "Password must be between 10 and 255 characters")
        String password
) {
}
