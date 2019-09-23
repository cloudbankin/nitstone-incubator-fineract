package org.finabile.fineract.portfolio.code.service;

import java.util.List;

import org.finabile.fineract.portfolio.code.data.FinabileCodeValueData;

public interface FinabileCodeValueReadPlatformService {

	public List<FinabileCodeValueData> getAddressTypes();

	public FinabileCodeValueData getAddressTypeValue(final Long valueId);
}
