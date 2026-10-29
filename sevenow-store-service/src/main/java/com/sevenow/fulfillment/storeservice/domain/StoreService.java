package com.sevenow.fulfillment.storeservice.domain;

import com.sevenow.fulfillment.domain.Product;
import com.sevenow.fulfillment.domain.Store;
import com.sevenow.fulfillment.domain.StoreCatalog;
import com.sevenow.fulfillment.domain.StoreRepository;
import com.sevenow.fulfillment.storeservice.events.CreateStoreRequest;
import com.sevenow.fulfillment.storeservice.events.StoreCatalogDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
public class StoreService {

  @Autowired
  private StoreRepository storeRepository;

  public Store create(CreateStoreRequest request) {
    Store store = new Store(request.getStoreNumber(), request.getName(), request.getAddress(), makeStoreCatalog(request.getCatalog()));
    storeRepository.save(store);
    return store;
  }

  private StoreCatalog makeStoreCatalog(StoreCatalogDTO catalog) {
    return new StoreCatalog(catalog.getProducts().stream().map(mi -> new Product(mi.getId(), mi.getName(), mi.getPrice(), mi.getOnHandQuantity(), mi.getSubstituteProductId())).collect(Collectors.toList()));
  }

  public Optional<Store> findById(long storeId) {
    return storeRepository.findById(storeId);
  }
}
