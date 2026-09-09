package com.princekombou.claimsapi.dto;

import com.princekombou.claimsapi.model.RiskFlag;
import java.time.LocalDateTime;

public record RiskFlagResponse(
    Long id,
    Long customerId,
    String category,
    RiskFlag.Severity severity,
    RiskFlag.Status status,
    String notes,
    LocalDateTime createdAt
) {}
