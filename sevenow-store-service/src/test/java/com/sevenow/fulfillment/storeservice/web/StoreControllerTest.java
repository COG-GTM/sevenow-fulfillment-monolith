package com.sevenow.fulfillment.storeservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.Money;
import com.sevenow.fulfillment.common.MoneyModule;
import com.sevenow.fulfillment.domain.Product;
import com.sevenow.fulfillment.domain.Store;
import com.sevenow.fulfillment.domain.StoreCatalog;
import com.sevenow.fulfillment.storeservice.domain.StoreService;
import com.sevenow.fulfillment.storeservice.events.CreateStoreRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

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

  private static final Address ADDRESS = new Address("3200 Hackberry Rd", null, "Irving", "TX", "75063");

  private StoreService storeService;
  private StoreController storeController;

  @Before
  public void setUp() {
    storeService = mock(StoreService.class);
    storeController = new StoreController();
    ReflectionTestUtils.setField(storeController, "storeService", storeService);
  }

  @Test
  public void shouldCreateStoreFromDocumentedCatalogProductsJson() {
    Store created = new Store(1L, "38114", "7-Eleven #38114", new StoreCatalog(Arrays.asList()));
    when(storeService.create(any(CreateStoreRequest.class))).thenReturn(created);

    String body = "{\n" +
            "  \"storeNumber\": \"38114\",\n" +
            "  \"name\": \"7-Eleven #38114\",\n" +
            "  \"address\": {\"street1\": \"3200 Hackberry Rd\", \"city\": \"Irving\", \"state\": \"TX\", \"zip\": \"75063\"},\n" +
            "  \"catalog\": {\n" +
            "    \"products\": [\n" +
            "      {\"id\": \"WATER-7SELECT-1L\", \"name\": \"7-Select Purified Water (1 L)\", \"price\": \"1.49\", \"onHandQuantity\": 60},\n" +
            "      {\"id\": \"PIZZA-PEPPERONI-WHOLE\", \"name\": \"Pepperoni Pizza (Whole)\", \"price\": \"7.99\", \"onHandQuantity\": 0, \"substituteProductId\": \"PIZZA-CHEESE-WHOLE\"}\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    given().
            standaloneSetup(configureControllers(storeController)).
            contentType("application/json").
            body(body).
    when().
            post("/stores").
    then().
            statusCode(200).
            body("id", equalTo(1));

    ArgumentCaptor<CreateStoreRequest> captor = ArgumentCaptor.forClass(CreateStoreRequest.class);
    verify(storeService).create(captor.capture());
    CreateStoreRequest request = captor.getValue();
    assertEquals("38114", request.getStoreNumber());
    assertEquals(2, request.getCatalog().getProducts().size());
    assertEquals("WATER-7SELECT-1L", request.getCatalog().getProducts().get(0).getId());
    assertEquals(60, request.getCatalog().getProducts().get(0).getOnHandQuantity());
    assertEquals("PIZZA-CHEESE-WHOLE", request.getCatalog().getProducts().get(1).getSubstituteProductId());
  }

  @Test
  public void shouldReturnStoreWithProductsInventoryAndSubstitutes() {
    Store store = new Store("38114", "7-Eleven #38114", ADDRESS, new StoreCatalog(Arrays.asList(
            new Product("WATER-7SELECT-1L", "7-Select Purified Water (1 L)", new Money("1.49"), 60),
            new Product("PIZZA-PEPPERONI-WHOLE", "Pepperoni Pizza (Whole)", new Money("7.99"), 0, "PIZZA-CHEESE-WHOLE"))));
    when(storeService.findById(1L)).thenReturn(Optional.of(store));

    given().
            standaloneSetup(configureControllers(storeController)).
    when().
            get("/stores/1").
    then().
            statusCode(200).
            body("storeNumber", equalTo("38114")).
            body("address.city", equalTo("Irving")).
            body("products[0].id", equalTo("WATER-7SELECT-1L")).
            body("products[0].onHandQuantity", equalTo(60)).
            body("products[0].substituteProductId", nullValue()).
            body("products[1].onHandQuantity", equalTo(0)).
            body("products[1].substituteProductId", equalTo("PIZZA-CHEESE-WHOLE"));
  }

  @Test
  public void shouldReturn404ForUnknownStore() {
    when(storeService.findById(99L)).thenReturn(Optional.empty());

    given().
            standaloneSetup(configureControllers(storeController)).
    when().
            get("/stores/99").
    then().
            statusCode(404);
  }

  private StandaloneMockMvcBuilder configureControllers(Object... controllers) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new MoneyModule());
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
    return MockMvcBuilders.standaloneSetup(controllers).setMessageConverters(converter);
  }
}
