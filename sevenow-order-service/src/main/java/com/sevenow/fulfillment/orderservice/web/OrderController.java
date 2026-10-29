package com.sevenow.fulfillment.orderservice.web;

import com.sevenow.fulfillment.domain.Order;
import com.sevenow.fulfillment.domain.OrderRepository;
import com.sevenow.fulfillment.domain.OrderRevision;
import com.sevenow.fulfillment.orderservice.api.web.CreateOrderRequest;
import com.sevenow.fulfillment.orderservice.api.web.CreateOrderResponse;
import com.sevenow.fulfillment.orderservice.api.web.OrderAcceptance;
import com.sevenow.fulfillment.orderservice.api.web.ReviseOrderRequest;
import com.sevenow.fulfillment.orderservice.domain.InvalidProductIdException;
import com.sevenow.fulfillment.orderservice.domain.OrderNotFoundException;
import com.sevenow.fulfillment.orderservice.domain.ProductOutOfStockException;
import com.sevenow.fulfillment.orderservice.domain.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(path = "/orders")
public class OrderController {

  private OrderService orderService;

  private OrderRepository orderRepository;


  public OrderController(OrderService orderService, OrderRepository orderRepository) {
    this.orderService = orderService;
    this.orderRepository = orderRepository;
  }

  @RequestMapping(method = RequestMethod.POST)
  public CreateOrderResponse create(@RequestBody CreateOrderRequest request) {
    Order order = orderService.createOrder(request.getCustomerId(),
            request.getStoreId(),
            request.getLineItems().stream().map(x -> new ProductIdAndQuantity(x.getProductId(), x.getQuantity())).collect(toList())
    );
    return new CreateOrderResponse(order.getId());
  }


  @RequestMapping(path = "/{orderId}", method = RequestMethod.GET)
  public ResponseEntity<GetOrderResponse> getOrder(@PathVariable long orderId) {
    Optional<Order> order = orderRepository.findById(orderId);
    return order.map(o -> new ResponseEntity<>(makeGetOrderResponse(o), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<List<GetOrderResponse>> getOrders(@RequestParam long customerId) {
    List<GetOrderResponse> orders = orderRepository.findAllByCustomerId(customerId)
            .stream()
            .map(this::makeGetOrderResponse)
            .collect(Collectors.toList());

    return new ResponseEntity<>(orders, HttpStatus.OK);
  }

  @ExceptionHandler(ProductOutOfStockException.class)
  public ResponseEntity<Map<String, Object>> handleOutOfStock(ProductOutOfStockException e) {
    Map<String, Object> body = new HashMap<>();
    body.put("message", e.getMessage());
    body.put("rejectedProductIds", e.getRejectedProductIds());
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidProductIdException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidProduct(InvalidProductIdException e) {
    Map<String, Object> body = new HashMap<>();
    body.put("message", e.getMessage());
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  private GetOrderResponse makeGetOrderResponse(Order order) {
    return new GetOrderResponse(order.getId(),
            order.getOrderState().name(),
            order.getOrderTotal(),
            order.getStore().getName(),
            order.getLineItems(),
            order.getAssignedDriver() == null ? null : order.getAssignedDriver().getId(),
            order.getAssignedDriver() == null ? null : order.getAssignedDriver().actionsForDelivery(order)
    );
  }

  @RequestMapping(path = "/{orderId}/cancel", method = RequestMethod.POST)
  public ResponseEntity<GetOrderResponse> cancel(@PathVariable long orderId) {
    try {
      Order order = orderService.cancel(orderId);
      return new ResponseEntity<>(makeGetOrderResponse(order), HttpStatus.OK);
    } catch (OrderNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(path = "/{orderId}/revise", method = RequestMethod.POST)
  public ResponseEntity<GetOrderResponse> revise(@PathVariable long orderId, @RequestBody ReviseOrderRequest request) {
    try {
      Order order = orderService.reviseOrder(orderId, new OrderRevision(Optional.empty(), request.getRevisedLineItemQuantities()));
      return new ResponseEntity<>(makeGetOrderResponse(order), HttpStatus.OK);
    } catch (OrderNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(path="/{orderId}/accept", method= RequestMethod.POST)
  public ResponseEntity<String> accept(@PathVariable long orderId, @RequestBody OrderAcceptance orderAcceptance) {
    orderService.accept(orderId, orderAcceptance.getReadyBy());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(path="/{orderId}/picking", method= RequestMethod.POST)
  public ResponseEntity<String> picking(@PathVariable long orderId) {
    orderService.notePicking(orderId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(path="/{orderId}/packed", method= RequestMethod.POST)
  public ResponseEntity<String> packed(@PathVariable long orderId) {
    orderService.notePacked(orderId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(path="/{orderId}/ready", method= RequestMethod.POST)
  public ResponseEntity<String> ready(@PathVariable long orderId) {
    orderService.noteReadyForPickup(orderId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(path="/{orderId}/pickedup", method= RequestMethod.POST)
  public ResponseEntity<String> pickedup(@PathVariable long orderId) {
    orderService.notePickedUp(orderId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(path="/{orderId}/delivered", method= RequestMethod.POST)
  public ResponseEntity<String> delivered(@PathVariable long orderId) {
    orderService.noteDelivered(orderId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

}
