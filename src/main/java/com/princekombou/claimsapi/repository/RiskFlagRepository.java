package com.princekombou.claimsapi.repository;

import com.princekombou.claimsapi.model.RiskFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskFlagRepository extends JpaRepository<RiskFlag, Long> {

    List<RiskFlag> findByCustomerId(Long customerId);

    List<RiskFlag> findByCustomerIdAndStatus(Long customerId, RiskFlag.Status status);
}
