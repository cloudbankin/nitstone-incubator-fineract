package org.apache.fineract.portfolio.charge.exception;

import java.math.BigDecimal;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class MaximumVolumeMustBeNullException extends AbstractPlatformResourceNotFoundException {

	public MaximumVolumeMustBeNullException(final BigDecimal currentMaxVolume) {
		super("Last Row Maximum Amount " + currentMaxVolume + " must be null", "" + 0);
	}
}
