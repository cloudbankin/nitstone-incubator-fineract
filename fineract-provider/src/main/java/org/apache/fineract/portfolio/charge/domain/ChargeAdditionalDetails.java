package org.apache.fineract.portfolio.charge.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;
/**
 * Habile has created this class
 * @author Hari
 *
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "hab_charge_additional_details")
public class ChargeAdditionalDetails extends AbstractPersistableCustom<Long> {
	
	@ManyToOne
	@JoinColumn(name = "charge_id",referencedColumnName = "id")
	private Charge charge;

	@Column(name = "is_enabled_fee_calculation_based_on_tenure")
	private Integer isEnabledFeeCalculationBasedOnTenure;

	@Column(name = "is_enabled_auto_paid")
	private Integer isEnabledAutoPaid;
	
	@Column(name = "is_tax_included")
	private Integer isTaxIncluded;
	
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
	
	@Column(name = "penalty_wait_period")
	private Integer penaltyWaitPeriod;
	
	@Column(name = "grace_on_penalty_posting")
	private Integer graceOnPenaltyPosting;
	
	
	public Integer getGraceOnPenaltyPosting() {
		return graceOnPenaltyPosting;
	}

	public void setGraceOnPenaltyPosting(Integer graceOnPenaltyPosting) {
		this.graceOnPenaltyPosting = graceOnPenaltyPosting;
	}

	public Integer getPenaltyWaitPeriod() {
		return penaltyWaitPeriod;
	}

	public void setPenaltyWaitPeriod(Integer penaltyWaitPeriod) {
		this.penaltyWaitPeriod = penaltyWaitPeriod;
	}

	public static ChargeAdditionalDetails fromJson(Charge charge, Integer isEnabledFeeCalculationBasedOnTenure,
			Integer isEnabledAutoPaid, Integer isTaxIncluded, Date createdDate, Date lastmodifiedDate, AppUser lastmodifiedById,
			AppUser createdById, Integer penaltyWaitPeriod, Integer graceOnPenaltyPosting) {
		return new ChargeAdditionalDetails(charge, isEnabledFeeCalculationBasedOnTenure, isEnabledAutoPaid,
				isTaxIncluded, createdDate, lastmodifiedDate, lastmodifiedById, createdById,penaltyWaitPeriod,
				graceOnPenaltyPosting);
	}
	
	public ChargeAdditionalDetails(Charge charge, Integer isEnabledFeeCalculationBasedOnTenure,
			Integer isEnabledAutoPaid, Integer isTaxIncluded, Date createdDate, Date lastmodifiedDate, AppUser lastmodifiedById,
			AppUser createdById,Integer penaltyWaitPeriod,Integer graceOnPenaltyPosting) {
		this.charge = charge;
		this.isEnabledFeeCalculationBasedOnTenure = isEnabledFeeCalculationBasedOnTenure;
		this.isEnabledAutoPaid = isEnabledAutoPaid;
		this.isTaxIncluded = isTaxIncluded;
		this.createdDate = createdDate;
		this.lastmodifiedDate = lastmodifiedDate;
		this.lastmodifiedById = lastmodifiedById;
		this.createdById = createdById;
		this.penaltyWaitPeriod = penaltyWaitPeriod;
		this.graceOnPenaltyPosting = graceOnPenaltyPosting;
	}
	
	public Charge getCharge() {
		return charge;
	}

	public void setCharge(Charge charge) {
		this.charge = charge;
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
}
