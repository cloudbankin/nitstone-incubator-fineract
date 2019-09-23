package org.finabile.fineract.portfolio.tax.service;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.loanproduct.domain.LendingStrategy;
import org.finabile.fineract.portfolio.tax.domain.TaxComponentType;

public class TaxComponentTypeEnumerations {
	public static EnumOptionData taxComponentType(final Integer id) {
		return taxComponentType(TaxComponentType.fromInt(id));
	}

	public static EnumOptionData taxComponentType(final String code) {
		return taxComponentType(TaxComponentType.fromString(code));
	}

	public static EnumOptionData taxComponentType(final TaxComponentType type) {
		EnumOptionData optionData = null;
		switch (type) {
		case INTER_STATE:
			optionData = new EnumOptionData(type.getValue().longValue(), type.getCode(), "Inter State");
			break;
		case INTRA_STATE:
			optionData = new EnumOptionData(type.getValue().longValue(), type.getCode(), "Intra State");
			break;

		default:
			optionData = new EnumOptionData(LendingStrategy.INVALID.getId().longValue(),
					LendingStrategy.INVALID.getCode(), "Invalid");
			break;

		}
		return optionData;
	}
}
