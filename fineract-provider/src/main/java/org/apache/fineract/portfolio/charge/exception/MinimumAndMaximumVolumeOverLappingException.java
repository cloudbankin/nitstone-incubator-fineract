package org.apache.fineract.portfolio.charge.exception;

import java.math.BigDecimal;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class MinimumAndMaximumVolumeOverLappingException extends AbstractPlatformResourceNotFoundException {

	public MinimumAndMaximumVolumeOverLappingException(final BigDecimal currentMinVolume,
			final BigDecimal beforeMaxVolume) {
		super("Minimum Amount " + currentMinVolume + " must be " + beforeMaxVolume, "", 0);
	}

}