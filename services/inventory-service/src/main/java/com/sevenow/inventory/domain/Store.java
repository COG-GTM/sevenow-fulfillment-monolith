package com.sevenow.inventory.domain;

import com.sevenow.inventory.common.Address;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "stores")
@Access(AccessType.FIELD)
@DynamicUpdate
public class Store {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String storeNumber;

  private String name;

  @Embedded
  private Address address;

  @Embedded
  @ElementCollection
  @CollectionTable(name = "store_products", joinColumns = @JoinColumn(name = "store_id"))
  private List<Product> products;

  public Store() {
  }

  public Store(String storeNumber, String name, Address address, StoreCatalog catalog) {
    this.storeNumber = storeNumber;
    this.name = name;
    this.address = address;
    this.products = catalog.getProducts();
  }

  public Store(Long id, String storeNumber, String name, Address address, StoreCatalog catalog) {
    this(storeNumber, name, address, catalog);
    this.id = id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStoreNumber() {
    return storeNumber;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public Address getAddress() {
    return address;
  }

  public Optional<Product> findProduct(String productId) {
    return products.stream().filter(mi -> mi.getId().equals(productId)).findFirst();
  }

  public List<Product> getProducts() {
    return products;
  }

}
