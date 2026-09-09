package com.princekombou.claimsapi.controller;

import com.princekombou.claimsapi.dto.*;
import com.princekombou.claimsapi.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse created = customerService.createCustomer(request);
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> listCustomers(Pageable pageable) {
        return ResponseEntity.ok(customerService.listCustomers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/risk-flags")
    public ResponseEntity<RiskFlagResponse> addRiskFlag(@PathVariable Long id, @Valid @RequestBody RiskFlagCreateRequest request) {
        RiskFlagResponse created = customerService.addRiskFlag(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/risk-flags")
    public ResponseEntity<List<RiskFlagResponse>> listRiskFlags(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.listRiskFlags(id));
    }

    @GetMapping("/{id}/risk-summary")
    public ResponseEntity<RiskSummaryResponse> getRiskSummary(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getRiskSummary(id));
    }
}
