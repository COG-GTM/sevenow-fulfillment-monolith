package com.sevenow.inventory.web;

import com.sevenow.inventory.domain.Product;
import com.sevenow.inventory.domain.Store;
import com.sevenow.inventory.domain.StoreCatalog;
import com.sevenow.inventory.domain.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/stores")
public class StoreController {

  private final StoreService storeService;

  public StoreController(StoreService storeService) {
    this.storeService = storeService;
  }

  @RequestMapping(method = RequestMethod.POST)
  public CreateStoreResponse create(@RequestBody CreateStoreRequest request) {
    StoreCatalog catalog = new StoreCatalog(request.getCatalog() == null || request.getCatalog().getProducts() == null
            ? Collections.emptyList()
            : request.getCatalog().getProducts().stream()
                .map(p -> new Product(p.getId(), p.getName(), p.getPrice(), p.getOnHandQuantity(), p.getSubstituteProductId()))
                .collect(Collectors.toList()));
    Store store = storeService.create(new Store(request.getStoreNumber(), request.getName(), request.getAddress(), catalog));
    return new CreateStoreResponse(store.getId());
  }

  @RequestMapping(method = RequestMethod.GET, path = "/{storeId}")
  public ResponseEntity<GetStoreResponse> get(@PathVariable long storeId) {
    return storeService.findById(storeId)
            .map(store -> ResponseEntity.ok(new GetStoreResponse(store)))
            .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @RequestMapping(method = RequestMethod.GET, path = "/{storeId}/products/{productId}")
  public ResponseEntity<Product> getProduct(@PathVariable long storeId, @PathVariable String productId) {
    return storeService.findProduct(storeId, productId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
  }

  public static class CreateStoreResponse {
    private final long id;

    public CreateStoreResponse(long id) {
      this.id = id;
    }

    public long getId() {
      return id;
    }
  }
}
