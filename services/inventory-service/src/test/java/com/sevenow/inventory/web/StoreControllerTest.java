package com.sevenow.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevenow.inventory.common.Address;
import com.sevenow.inventory.common.Money;
import com.sevenow.inventory.common.MoneyModule;
import com.sevenow.inventory.domain.Product;
import com.sevenow.inventory.domain.Store;
import com.sevenow.inventory.domain.StoreCatalog;
import com.sevenow.inventory.domain.StoreService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Optional;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StoreControllerTest {

  static final Address ADDRESS = new Address("3200 Hackberry Rd", null, "Irving", "TX", "75063");

  private StoreService storeService;

  @Before
  public void setUp() {
    storeService = mock(StoreService.class);
    StoreController controller = new StoreController(storeService);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new MoneyModule());
    io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)));
  }

  @Test
  public void shouldCreateStoreFromCatalogProductsJson() {
    when(storeService.create(any(Store.class))).thenReturn(new Store(1L, "38114", "7-Eleven #38114", ADDRESS, new StoreCatalog(Arrays.asList())));

    String body = "{\"storeNumber\": \"38114\", \"name\": \"7-Eleven #38114\"," +
            " \"address\": {\"street1\": \"3200 Hackberry Rd\", \"city\": \"Irving\", \"state\": \"TX\", \"zip\": \"75063\"}," +
            " \"catalog\": {\"products\": [" +
            "  {\"id\": \"PIZZA-PEPPERONI-WHOLE\", \"name\": \"Pepperoni Pizza (Whole)\", \"price\": \"7.99\", \"onHandQuantity\": 0, \"substituteProductId\": \"PIZZA-CHEESE-WHOLE\"}]}}";

    given().contentType("application/json").body(body)
            .when().post("/stores")
            .then().statusCode(200).body("id", equalTo(1));

    ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
    verify(storeService).create(captor.capture());
    Product product = captor.getValue().getProducts().get(0);
    assertEquals("38114", captor.getValue().getStoreNumber());
    assertEquals(0, product.getOnHandQuantity());
    assertEquals("PIZZA-CHEESE-WHOLE", product.getSubstituteProductId());
  }

  @Test
  public void shouldReturnCatalogWithOnHandQuantityAndSubstitute() {
    when(storeService.findById(1L)).thenReturn(Optional.of(new Store(1L, "38114", "7-Eleven #38114", ADDRESS, new StoreCatalog(Arrays.asList(
            new Product("WINGS-8PC", "Chicken Wings (8 pc)", new Money("6.49"), 0),
            new Product("RED-BULL-12OZ", "Red Bull Energy Drink (12 oz)", new Money("3.49"), 0, "MONSTER-16OZ"))))));

    given().when().get("/stores/1")
            .then().statusCode(200)
            .body("storeNumber", equalTo("38114"))
            .body("address.city", equalTo("Irving"))
            .body("products[0].substituteProductId", nullValue())
            .body("products[1].price", equalTo("3.49"))
            .body("products[1].onHandQuantity", equalTo(0))
            .body("products[1].substituteProductId", equalTo("MONSTER-16OZ"));
  }

  @Test
  public void shouldReturnProductForPriceAndStockLookup() {
    when(storeService.findProduct(1L, "MONSTER-16OZ")).thenReturn(Optional.of(new Product("MONSTER-16OZ", "Monster Energy (16 oz)", new Money("2.99"), 24)));

    given().when().get("/stores/1/products/MONSTER-16OZ")
            .then().statusCode(200)
            .body("id", equalTo("MONSTER-16OZ"))
            .body("price", equalTo("2.99"))
            .body("onHandQuantity", equalTo(24));
  }

  @Test
  public void shouldReturn404ForUnknownStoreOrProduct() {
    when(storeService.findById(99L)).thenReturn(Optional.empty());
    when(storeService.findProduct(1L, "NOPE")).thenReturn(Optional.empty());

    given().when().get("/stores/99").then().statusCode(404);
    given().when().get("/stores/1/products/NOPE").then().statusCode(404);
  }
}
