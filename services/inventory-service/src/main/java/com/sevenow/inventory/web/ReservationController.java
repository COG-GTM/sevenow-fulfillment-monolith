package com.sevenow.inventory.web;

import com.sevenow.inventory.domain.Reservation;
import com.sevenow.inventory.domain.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/reservations")
public class ReservationController {

  private final ReservationService reservationService;

  public ReservationController(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  /** Dry run of the substitution rule: what {@code POST /reservations} would hand out, without taking stock. */
  @RequestMapping(method = RequestMethod.POST, path = "/resolve")
  public ResolveResponse resolve(@RequestBody ItemsRequest request) {
    return new ResolveResponse(request.getStoreId(), reservationService.resolve(request.getStoreId(), request.getItems()));
  }

  @RequestMapping(method = RequestMethod.POST)
  public ResponseEntity<Reservation> reserve(@RequestBody ItemsRequest request) {
    return new ResponseEntity<>(reservationService.reserve(request.getStoreId(), request.getItems()), HttpStatus.CREATED);
  }

  @RequestMapping(method = RequestMethod.POST, path = "/{reservationId}/release")
  public Reservation release(@PathVariable long reservationId) {
    return reservationService.release(reservationId);
  }

  @RequestMapping(method = RequestMethod.GET, path = "/{reservationId}")
  public ResponseEntity<Reservation> get(@PathVariable long reservationId) {
    return reservationService.findById(reservationId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
