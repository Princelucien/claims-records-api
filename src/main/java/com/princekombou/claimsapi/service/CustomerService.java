package com.princekombou.claimsapi.service;

import com.princekombou.claimsapi.dto.*;
import com.princekombou.claimsapi.exception.DuplicateEmailException;
import com.princekombou.claimsapi.exception.ResourceNotFoundException;
import com.princekombou.claimsapi.model.Customer;
import com.princekombou.claimsapi.model.RiskFlag;
import com.princekombou.claimsapi.repository.CustomerRepository;
import com.princekombou.claimsapi.repository.RiskFlagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RiskFlagRepository riskFlagRepository;

    public CustomerService(CustomerRepository customerRepository, RiskFlagRepository riskFlagRepository) {
        this.customerRepository = customerRepository;
        this.riskFlagRepository = riskFlagRepository;
    }

    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("A customer with email " + request.email() + " already exists");
        }
        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());
        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        return toResponse(findCustomerOrThrow(id));
    }

    public void deleteCustomer(Long id) {
        Customer customer = findCustomerOrThrow(id);
        customerRepository.delete(customer);
    }

    public RiskFlagResponse addRiskFlag(Long customerId, RiskFlagCreateRequest request) {
        Customer customer = findCustomerOrThrow(customerId);

        RiskFlag flag = new RiskFlag();
        flag.setCustomer(customer);
        flag.setCategory(request.category());
        flag.setSeverity(request.severity());
        flag.setNotes(request.notes());

        RiskFlag saved = riskFlagRepository.save(flag);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RiskFlagResponse> listRiskFlags(Long customerId) {
        findCustomerOrThrow(customerId);
        return riskFlagRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RiskSummaryResponse getRiskSummary(Long customerId) {
        findCustomerOrThrow(customerId);
        List<RiskFlag> openFlags = riskFlagRepository.findByCustomerIdAndStatus(customerId, RiskFlag.Status.OPEN);

        Map<String, Long> bySeverity = openFlags.stream()
                .collect(Collectors.groupingBy(f -> f.getSeverity().name(), Collectors.counting()));

        return new RiskSummaryResponse(customerId, openFlags.size(), bySeverity);
    }

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + id + " not found"));
    }

    private CustomerResponse toResponse(Customer customer) {
        long openCount = customer.getRiskFlags() == null ? 0 :
                customer.getRiskFlags().stream().filter(f -> f.getStatus() == RiskFlag.Status.OPEN).count();
        return new CustomerResponse(customer.getId(), customer.getFullName(), customer.getEmail(), customer.getCreatedAt(), openCount);
    }

    private RiskFlagResponse toResponse(RiskFlag flag) {
        return new RiskFlagResponse(flag.getId(), flag.getCustomer().getId(), flag.getCategory(), flag.getSeverity(), flag.getStatus(), flag.getNotes(), flag.getCreatedAt());
    }
}
