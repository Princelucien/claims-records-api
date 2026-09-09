package com.princekombou.claimsapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
    @NotBlank(message = "fullName is required")
    @Size(max = 160)
    String fullName,

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 160)
    String email
) {}
