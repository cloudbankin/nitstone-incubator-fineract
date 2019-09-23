package org.finabile.fineract.portfolio.tax.domain;

public enum TaxComponentType {
	INTRA_STATE(0, "taxComponentType.intra_state"), //
	INTER_STATE(1, "taxComponentType.inter_state"), //
	INVALID(2, "taxComponentType.invalid");

	private final Integer value;
	private final String code;

	private TaxComponentType(final Integer value, final String code) {
		this.value = value;
		this.code = code;
	}

	public Integer getValue() {
		return this.value;
	}

	public String getCode() {
		return this.code;
	}

	public static TaxComponentType fromInt(final Integer selectedMethod) {

		if (selectedMethod == null) {
			return null;
		}

		TaxComponentType repaymentMethod = null;
		switch (selectedMethod) {
		case 0:
			repaymentMethod = TaxComponentType.INTRA_STATE;
			break;
		case 1:
			repaymentMethod = TaxComponentType.INTER_STATE;
			break;
		default:
			repaymentMethod = TaxComponentType.INVALID;
			break;
		}
		return repaymentMethod;
	}

	public static TaxComponentType fromString(final String selectedMethod) {

		if (selectedMethod == null) {
			return null;
		}

		TaxComponentType repaymentMethod = null;
		switch (selectedMethod) {
		case "Intra State":
			repaymentMethod = TaxComponentType.INTRA_STATE;
			break;
		case "Inter State":
			repaymentMethod = TaxComponentType.INTER_STATE;
			break;
		default:
			repaymentMethod = TaxComponentType.INVALID;
			break;
		}
		return repaymentMethod;
	}

	public boolean isInterState() {
		return this.value.equals(TaxComponentType.INTER_STATE.getValue());
	}

	public boolean isIntraState() {
		return this.value.equals(TaxComponentType.INTRA_STATE.getValue());
	}
}
