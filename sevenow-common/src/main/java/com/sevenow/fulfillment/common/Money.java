package com.sevenow.fulfillment.common;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
@Access(AccessType.FIELD)
public class Money {

  public static Money ZERO = new Money(0);

  public static final int MAX_STRING_LENGTH = 32;
  public static final int MAX_SCALE = 4;
  public static final int MAX_INTEGER_DIGITS = 17;

  private BigDecimal amount;

  private Money() {
  }

  public Money(BigDecimal amount) {
    this.amount = amount;
  }

  public Money(String s) {
    if (s.length() > MAX_STRING_LENGTH)
      throw new IllegalArgumentException("Money amount must be at most " + MAX_STRING_LENGTH + " characters");
    this.amount = requireBounded(new BigDecimal(s));
  }

  private static BigDecimal requireBounded(BigDecimal amount) {
    long integerDigits = (long) amount.precision() - amount.scale();
    if (amount.scale() > MAX_SCALE || integerDigits > MAX_INTEGER_DIGITS)
      throw new IllegalArgumentException("Money amount must have at most " + MAX_INTEGER_DIGITS +
              " integer digits and " + MAX_SCALE + " decimal places");
    return amount;
  }

  public Money(int i) {
    this.amount = new BigDecimal(i);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    Money money = (Money) o;

    return new EqualsBuilder()
            .append(amount, money.amount)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(amount)
            .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("amount", amount)
            .toString();
  }


  public Money add(Money delta) {
    return new Money(amount.add(delta.amount));
  }

  public boolean isGreaterThanOrEqual(Money other) {
    return amount.compareTo(other.amount) >= 0;
  }

  public String asString() {
    return amount.toPlainString();
  }

  public Money multiply(int x) {
    return new Money(amount.multiply(new BigDecimal(x)));
  }

}
