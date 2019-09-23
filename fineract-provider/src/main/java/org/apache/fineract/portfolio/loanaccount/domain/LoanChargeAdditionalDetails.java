package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@SuppressWarnings("serial")
@Entity
@Table(name = "hab_loan_charge_additional_details")
public class LoanChargeAdditionalDetails extends AbstractPersistableCustom<Long> {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loan_charge_id", referencedColumnName = "id")
	private LoanCharge loanCharge;

	@Column(name = "fees_component")
	private BigDecimal feesComponent;

	@Column(name = "tax_component")
	private BigDecimal taxComponent;

	@Column(name = "tax_component_type")
	private String taxComponentType;

	@Column(name = "is_enabled_fee_calculation_based_on_tenure")
	private Integer isEnabledFeeCalculationBasedOnTenure;

	@Column(name = "is_enabled_auto_paid")
	private Integer isEnabledAutoPaid;

	@Column(name = "is_tax_included")
	private Integer isTaxIncluded;

	@Column(name = "number_of_days")
	private Integer numberOfDays;

	@ManyToOne
	@JoinColumn(name = "createdby_id")
	private AppUser createdById;

	@Temporal(TemporalType.DATE)
	@Column(name = "created_date")
	private Date createdDate;

	@ManyToOne
	@JoinColumn(name = "lastmodifiedby_id")
	private AppUser lastmodifiedById;

	@Temporal(TemporalType.DATE)
	@Column(name = "lastmodified_date")
	private Date lastmodifiedDate;

	public static LoanChargeAdditionalDetails fromJson(LoanCharge loanCharge,
			Integer isEnabledFeeCalculationBasedOnTenure, Integer isEnabledAutoPaid, Integer isTaxIncluded,
			Date createdDate, Date lastmodifiedDate, AppUser lastmodifiedById, AppUser createdById,
			Integer numberOfDays) {
		return new LoanChargeAdditionalDetails(loanCharge, isEnabledFeeCalculationBasedOnTenure, isEnabledAutoPaid,
				isTaxIncluded, createdDate, lastmodifiedDate, lastmodifiedById, createdById, numberOfDays);
	}

	public LoanChargeAdditionalDetails(LoanCharge loanCharge, Integer isEnabledFeeCalculationBasedOnTenure,
			Integer isEnabledAutoPaid, Integer isTaxIncluded, Date createdDate, Date lastmodifiedDate,
			AppUser lastmodifiedById, AppUser createdById, Integer numberOfDays) {
		this.loanCharge = loanCharge;
		this.isEnabledFeeCalculationBasedOnTenure = isEnabledFeeCalculationBasedOnTenure;
		this.isEnabledAutoPaid = isEnabledAutoPaid;
		this.isTaxIncluded = isTaxIncluded;
		this.createdDate = createdDate;
		this.lastmodifiedDate = lastmodifiedDate;
		this.lastmodifiedById = lastmodifiedById;
		this.createdById = createdById;
		this.numberOfDays = numberOfDays;
	}

	public LoanCharge getLoanCharge() {
		return loanCharge;
	}

	public void setLoanCharge(LoanCharge loanCharge) {
		this.loanCharge = loanCharge;
	}

	public Integer getIsEnabledFeeCalculationBasedOnTenure() {
		return isEnabledFeeCalculationBasedOnTenure;
	}

	public void setIsEnabledFeeCalculationBasedOnTenure(Integer isEnabledFeeCalculationBasedOnTenure) {
		this.isEnabledFeeCalculationBasedOnTenure = isEnabledFeeCalculationBasedOnTenure;
	}

	public AppUser getCreatedById() {
		return createdById;
	}

	public void setCreatedById(AppUser createdById) {
		this.createdById = createdById;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public AppUser getLastmodifiedById() {
		return lastmodifiedById;
	}

	public void setLastmodifiedById(AppUser lastmodifiedById) {
		this.lastmodifiedById = lastmodifiedById;
	}

	public Date getLastmodifiedDate() {
		return lastmodifiedDate;
	}

	public void setLastmodifiedDate(Date lastmodifiedDate) {
		this.lastmodifiedDate = lastmodifiedDate;
	}

	public Integer getNumberOfDays() {
		return numberOfDays;
	}

	public void setNumberOfDays(Integer numberOfDays) {
		this.numberOfDays = numberOfDays;
	}

	public Integer getIsEnabledAutoPaid() {
		return isEnabledAutoPaid;
	}

	public void setIsEnabledAutoPaid(Integer isEnabledAutoPaid) {
		this.isEnabledAutoPaid = isEnabledAutoPaid;
	}

	public Integer getIsTaxIncluded() {
		return isTaxIncluded;
	}

	public void setIsTaxIncluded(Integer isTaxIncluded) {
		this.isTaxIncluded = isTaxIncluded;
	}

	public BigDecimal getFeesComponent() {
		return feesComponent;
	}

	public void setFeesComponent(BigDecimal feesComponent) {
		this.feesComponent = feesComponent;
	}

	public BigDecimal getTaxComponent() {
		return taxComponent;
	}

	public void setTaxComponent(BigDecimal taxComponent) {
		this.taxComponent = taxComponent;
	}

	public String getTaxComponentType() {
		return taxComponentType;
	}

	public void setTaxComponentType(String taxComponentType) {
		this.taxComponentType = taxComponentType;
	}

}
