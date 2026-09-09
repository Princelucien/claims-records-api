package com.princekombou.claimsapi.dto;

import java.time.LocalDateTime;

public record CustomerResponse(
    Long id,
    String fullName,
    String email,
    LocalDateTime createdAt,
    long openRiskFlagCount
) {}
