package com.princekombou.claimsapi.dto;

import com.princekombou.claimsapi.model.RiskFlag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RiskFlagCreateRequest(
    @NotBlank @Size(max = 60)
    String category,

    @NotNull(message = "severity is required")
    RiskFlag.Severity severity,

    String notes
) {}
