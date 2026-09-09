package com.princekombou.claimsapi.dto;

import java.util.Map;

public record RiskSummaryResponse(
    Long customerId,
    long openFlagCount,
    Map<String, Long> openBySeverity
) {}
