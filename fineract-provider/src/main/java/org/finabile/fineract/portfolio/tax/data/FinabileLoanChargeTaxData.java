package org.finabile.fineract.portfolio.tax.data;

import java.math.BigDecimal;

public class FinabileLoanChargeTaxData {

	public Long loanChargeId;
	public String taxComponentName;
	public BigDecimal taxComponentAmount;
	
	public FinabileLoanChargeTaxData(Long loanChargeId, String taxComponentType,
			BigDecimal taxComponentAmount) {
		this.loanChargeId = loanChargeId;
		this.taxComponentName = taxComponentType;
		this.taxComponentAmount = taxComponentAmount;
	}

	public Long getLoanChargeId() {
		return loanChargeId;
	}

	public void setLoanChargeId(Long loanChargeId) {
		this.loanChargeId = loanChargeId;
	}

	public String getTaxComponentName() {
		return taxComponentName;
	}

	public void setTaxComponentName(String taxComponentName) {
		this.taxComponentName = taxComponentName;
	}

	public BigDecimal getTaxComponentAmount() {
		return taxComponentAmount;
	}

	public void setTaxComponentAmount(BigDecimal taxComponentAmount) {
		this.taxComponentAmount = taxComponentAmount;
	}
	
}
