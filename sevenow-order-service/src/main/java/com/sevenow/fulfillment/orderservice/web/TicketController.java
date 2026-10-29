package com.sevenow.fulfillment.orderservice.web;

import com.sevenow.fulfillment.orderservice.api.web.OrderAcceptance;
import com.sevenow.fulfillment.orderservice.domain.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TicketController {

  private OrderService orderService;

  public TicketController(OrderService orderService) {
    this.orderService = orderService;
  }


}
