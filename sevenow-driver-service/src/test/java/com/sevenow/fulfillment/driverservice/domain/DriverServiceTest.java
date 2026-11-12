package com.sevenow.fulfillment.driverservice.domain;

import com.sevenow.fulfillment.common.Address;
import com.sevenow.fulfillment.common.PersonName;
import com.sevenow.fulfillment.domain.Driver;
import com.sevenow.fulfillment.domain.DriverRepository;
import com.sevenow.fulfillment.security.SevenowPrincipal;
import com.sevenow.fulfillment.security.SevenowRole;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class DriverServiceTest {

  private static final long DRIVER_ID = 1L;
  private static final long OTHER_DRIVER_ID = 2L;

  private DriverRepository driverRepository;
  private DriverService driverService;
  private Driver driver;

  @Before
  public void setUp() {
    driverRepository = mock(DriverRepository.class);
    driverService = new DriverService(driverRepository);
    driver = new Driver(new PersonName("Dana", "Driver"), new Address("5601 N MacArthur Blvd", null, "Irving", "TX", "75038"));
    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(driverRepository.findById(OTHER_DRIVER_ID)).thenReturn(Optional.of(
            new Driver(new PersonName("Marco", "Mendoza"), new Address("7701 Las Colinas Ridge", null, "Irving", "TX", "75063"))));
  }

  @After
  public void clearCaller() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void shouldRequireAuthenticationForEveryOperation() {
    assertThatThrownBy(() -> driverService.createDriver(new PersonName("Mallory", "M"), null))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    assertThatThrownBy(() -> driverService.updateAvailability(DRIVER_ID, false))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    assertThatThrownBy(() -> driverService.findDriverForCaller(DRIVER_ID))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    assertThatThrownBy(() -> driverService.approve(DRIVER_ID))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    verify(driverRepository, never()).save(any());
    verify(driverRepository, never()).findById(anyLong());
  }

  @Test
  public void shouldOnlyLetDispatcherRegisterAndApproveDrivers() {
    for (SevenowRole role : new SevenowRole[]{SevenowRole.DRIVER, SevenowRole.CUSTOMER, SevenowRole.STORE_ASSOCIATE, SevenowRole.STORE_ADMIN}) {
      actAs(role, DRIVER_ID);
      assertThatThrownBy(() -> driverService.createDriver(new PersonName("Mallory", "M"), null))
              .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> driverService.approve(DRIVER_ID))
              .isInstanceOf(AccessDeniedException.class);
    }
    verify(driverRepository, never()).save(any());
    assertThat(driver.isApproved()).isFalse();

    actAs(SevenowRole.DISPATCHER, 0);
    Driver created = driverService.createDriver(new PersonName("Ana", "Lopez"), null);
    verify(driverRepository).save(created);
    assertThat(created.isApproved()).isFalse();
    assertThat(created.isAvailable()).isFalse();

    driverService.approve(DRIVER_ID);
    assertThat(driver.isApproved()).isTrue();
  }

  @Test
  public void shouldOnlyLetDriverItselfOrDispatcherChangeAvailability() {
    actAs(SevenowRole.DRIVER, OTHER_DRIVER_ID);
    assertThatThrownBy(() -> driverService.updateAvailability(DRIVER_ID, true))
            .isInstanceOf(AccessDeniedException.class);
    actAs(SevenowRole.CUSTOMER, DRIVER_ID);
    assertThatThrownBy(() -> driverService.updateAvailability(DRIVER_ID, true))
            .isInstanceOf(AccessDeniedException.class);
    assertThat(driver.isAvailable()).isFalse();

    actAs(SevenowRole.DRIVER, DRIVER_ID);
    driverService.updateAvailability(DRIVER_ID, true);
    assertThat(driver.isAvailable()).isTrue();

    actAs(SevenowRole.DISPATCHER, 0);
    driverService.updateAvailability(DRIVER_ID, false);
    assertThat(driver.isAvailable()).isFalse();
  }

  @Test
  public void shouldOnlyShowDriverToItselfOrDispatcher() {
    actAs(SevenowRole.DRIVER, OTHER_DRIVER_ID);
    assertThatThrownBy(() -> driverService.findDriverForCaller(DRIVER_ID))
            .isInstanceOf(AccessDeniedException.class);
    actAs(SevenowRole.STORE_ASSOCIATE, DRIVER_ID);
    assertThatThrownBy(() -> driverService.findDriverForCaller(DRIVER_ID))
            .isInstanceOf(AccessDeniedException.class);
    verify(driverRepository, never()).findById(DRIVER_ID);

    actAs(SevenowRole.DRIVER, DRIVER_ID);
    assertThat(driverService.findDriverForCaller(DRIVER_ID)).isSameAs(driver);
    actAs(SevenowRole.DISPATCHER, 0);
    assertThat(driverService.findDriverForCaller(DRIVER_ID)).isSameAs(driver);
  }

  @Test
  public void shouldReportUnknownDriverAsNotFound() {
    when(driverRepository.findById(99L)).thenReturn(Optional.empty());
    actAs(SevenowRole.DISPATCHER, 0);
    assertThatThrownBy(() -> driverService.findDriverForCaller(99L)).isInstanceOf(DriverNotFoundException.class);
    assertThatThrownBy(() -> driverService.updateAvailability(99L, true)).isInstanceOf(DriverNotFoundException.class);
    assertThatThrownBy(() -> driverService.approve(99L)).isInstanceOf(DriverNotFoundException.class);
  }

  private static void actAs(SevenowRole role, long id) {
    SevenowPrincipal principal = new SevenowPrincipal(role.name().toLowerCase() + "-" + id, role, id);
    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
  }
}
