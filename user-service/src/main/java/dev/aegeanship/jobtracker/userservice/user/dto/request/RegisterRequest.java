package dev.aegeanship.jobtracker.userservice.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Username is required")
        String username ,

        @NotBlank(message = "First name is required")
        String firstName ,

        @NotBlank(message = "Last name is required")
        String lastName ,

        @Email
        @NotBlank(message = "Email is required")
        String email ,

        @NotBlank(message = "Phone number is required")
        String phoneNumber ,

        @Size(min = 10, max = 255 , message = "Password must be between 10 and 255 characters")
        @NotBlank(message = "Password is required")
        String password


) {
}
