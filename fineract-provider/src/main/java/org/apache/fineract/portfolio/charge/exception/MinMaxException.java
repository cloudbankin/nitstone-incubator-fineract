package org.apache.fineract.portfolio.charge.exception;

import java.math.BigDecimal;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class MinMaxException extends AbstractPlatformResourceNotFoundException {

	public MinMaxException(final BigDecimal currentAmount, final BigDecimal afterAmount) {
		super("Minimum Amount " + afterAmount + " must be " + currentAmount, "" + 0);
	}

}
