package com.sevenow.fulfillment.storeservice.web;

import com.sevenow.fulfillment.domain.Store;
import com.sevenow.fulfillment.storeservice.domain.StoreService;
import com.sevenow.fulfillment.storeservice.events.CreateStoreRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/stores")
public class StoreController {

  @Autowired
  private StoreService storeService;

  @RequestMapping(method = RequestMethod.POST)
  public CreateStoreResponse create(@RequestBody CreateStoreRequest request) {
    Store r = storeService.create(request);
    return new CreateStoreResponse(r.getId());
  }

  @RequestMapping(method = RequestMethod.GET, path = "/{storeId}")
  public ResponseEntity<GetStoreResponse> get(@PathVariable long storeId) {
    return storeService.findById(storeId)
            .map(r -> new ResponseEntity<>(makeGetStoreResponse(r), HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  private GetStoreResponse makeGetStoreResponse(Store r) {
    return new GetStoreResponse(r.getId(), r.getStoreNumber(), r.getName(), r.getAddress(), r.getProducts());
  }


}
