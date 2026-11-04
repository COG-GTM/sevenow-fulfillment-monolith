package com.sevenow.fulfillment.customerservice.domain;

import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.common.PersonName;
import com.sevenow.fulfillment.domain.Customer;
import com.sevenow.fulfillment.domain.CustomerRepository;
import com.sevenow.fulfillment.security.SevenowCaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class CustomerService {

  @Autowired
  private CustomerRepository customerRepository;

  public void validateOrderForCustomer(long customerId, Money orderTotal) {
    Optional<Customer> customer = customerRepository.findById(customerId);
    customer.orElseThrow(CustomerNotFoundException::new).validateOrderByCustomer(orderTotal);
  }

  public Customer create(PersonName name) {
    Customer customer = customerRepository.save(new Customer(name));
    return customer;
  }

  /**
   * The customer's profile, readable only by that customer.
   */
  public Optional<Customer> findCustomerForCaller(long customerId) {
    SevenowCaller.requireCustomer(customerId);
    return customerRepository.findById(customerId);
  }
}
