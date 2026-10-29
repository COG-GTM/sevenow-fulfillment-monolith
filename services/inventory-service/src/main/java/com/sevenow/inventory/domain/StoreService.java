package com.sevenow.inventory.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StoreService {

  private final StoreRepository storeRepository;

  public StoreService(StoreRepository storeRepository) {
    this.storeRepository = storeRepository;
  }

  public Store create(Store store) {
    return storeRepository.save(store);
  }

  @Transactional(readOnly = true)
  public Optional<Store> findById(long storeId) {
    return storeRepository.findById(storeId);
  }

  @Transactional(readOnly = true)
  public Optional<Product> findProduct(long storeId, String productId) {
    return storeRepository.findById(storeId).flatMap(s -> s.findProduct(productId));
  }
}
