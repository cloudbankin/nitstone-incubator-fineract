package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.tax.domain.TaxComponent;

@SuppressWarnings("serial")
@Entity
@Table(name = "hab_loan_charge_tax_details")
public class FinabileLoanChargeTaxDetails extends AbstractPersistableCustom<Long> {

	@ManyToOne(optional = false)
	@JoinColumn(name = "loan_charge_id", referencedColumnName = "id", nullable= false)
	private LoanCharge loanCharge;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "tax_component_id", referencedColumnName = "id", nullable= false)
	private TaxComponent taxComponent;

	@Column(name = "tax_component_name")
	private String taxComponentName;

	@Column(name = "tax_component_amount")
	private BigDecimal taxComponentAmount;
	
	@Column(name = "tax_component_accounted")
	private BigDecimal taxComponentAmountAccounted;

	public static FinabileLoanChargeTaxDetails fromJson(LoanCharge loanCharge, TaxComponent taxComponent, final String taxComponentName,
			final BigDecimal taxComponentAmount) {
		return new FinabileLoanChargeTaxDetails(loanCharge, taxComponent, taxComponentName, taxComponentAmount);
	}

	public FinabileLoanChargeTaxDetails(LoanCharge loanCharge, TaxComponent taxComponent, final String taxComponentName,
			final BigDecimal taxComponentAmount) {
		this.loanCharge = loanCharge;
		this.taxComponentName = taxComponentName;
		this.taxComponentAmount = taxComponentAmount;
		this.taxComponent = taxComponent;
	}

	public LoanCharge getLoanCharge() {
		return loanCharge;
	}

	public void setLoanCharge(LoanCharge loanCharge) {
		this.loanCharge = loanCharge;
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

	public TaxComponent getTaxComponent() {
		return taxComponent;
	}

	public void setTaxComponent(TaxComponent taxComponent) {
		this.taxComponent = taxComponent;
	}

	public BigDecimal getTaxComponentAmountAccounted() {
		return taxComponentAmountAccounted;
	}

	public void setTaxComponentAmountAccounted(BigDecimal taxComponentAmountAccounted) {
		this.taxComponentAmountAccounted = taxComponentAmountAccounted;
	}
	
	
}
