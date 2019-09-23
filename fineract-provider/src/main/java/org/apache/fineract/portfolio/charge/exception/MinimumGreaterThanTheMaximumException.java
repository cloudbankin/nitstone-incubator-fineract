package org.apache.fineract.portfolio.charge.exception;

import java.math.BigDecimal;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

/**
 * This exception class is used to throw min amount greater than the max amount exception
 * @author Habile
 *
 */
public class MinimumGreaterThanTheMaximumException extends AbstractPlatformResourceNotFoundException {

	public MinimumGreaterThanTheMaximumException(final BigDecimal minVolume, final BigDecimal maxVolume) {
		super("Maximum Amount " + maxVolume + " must be greater than the Minimum Amount " + minVolume, "" + 0);
	}

}
