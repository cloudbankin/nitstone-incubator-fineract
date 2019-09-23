/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.exception.LoanChargeWithoutMandatoryFieldException;
import org.apache.fineract.portfolio.loanaccount.command.LoanChargeCommand;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargePaidDetail;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformServiceImpl;
import org.apache.fineract.portfolio.tax.domain.TaxGroupMappings;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetails;

@Entity
@Table(name = "m_loan_charge")
public class LoanCharge extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", referencedColumnName = "id", nullable = false)
    private Loan loan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "charge_id", referencedColumnName = "id", nullable = false)
    private Charge charge;

    @Column(name = "charge_time_enum", nullable = false)
    private Integer chargeTime;

    @Temporal(TemporalType.DATE)
    @Column(name = "due_for_collection_as_of_date")
    private Date dueDate;

    @Column(name = "charge_calculation_enum")
    private Integer chargeCalculation;

    @Column(name = "charge_payment_mode_enum")
    private Integer chargePaymentMode;

    @Column(name = "calculation_percentage", scale = 6, precision = 19, nullable = true)
    private BigDecimal percentage;

    @Column(name = "calculation_on_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountPercentageAppliedTo;

    @Column(name = "charge_amount_or_percentage", scale = 6, precision = 19, nullable = false)
    private BigDecimal amountOrPercentage;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_paid_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountPaid;

    @Column(name = "amount_waived_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountWaived;

    @Column(name = "amount_writtenoff_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountWrittenOff;

    @Column(name = "amount_outstanding_derived", scale = 6, precision = 19, nullable = false)
    private BigDecimal amountOutstanding;

    @Column(name = "is_penalty", nullable = false)
    private boolean penaltyCharge = false;

    @Column(name = "is_paid_derived", nullable = false)
    private boolean paid = false;

    @Column(name = "waived", nullable = false)
    private boolean waived = false;

    @Column(name = "min_cap", scale = 6, precision = 19, nullable = true)
    private BigDecimal minCap;

    @Column(name = "max_cap", scale = 6, precision = 19, nullable = true)
    private BigDecimal maxCap;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loancharge", orphanRemoval = true, fetch=FetchType.EAGER)
    private Set<LoanInstallmentCharge> loanInstallmentCharge = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "loancharge", cascade = CascadeType.ALL, optional = true, orphanRemoval = true, fetch = FetchType.EAGER)
    private LoanOverdueInstallmentCharge overdueInstallmentCharge;

    @OneToOne(mappedBy = "loancharge", cascade = CascadeType.ALL, optional = true, orphanRemoval = true, fetch = FetchType.EAGER)
    private LoanTrancheDisbursementCharge loanTrancheDisbursementCharge;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loanCharge", orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<FinabileLoanChargeTaxDetails> finabileLoanChargeTaxDetails = new HashSet<>();

	public Set<FinabileLoanChargeTaxDetails> getFinabileLoanChargeTaxDetails() {
		return finabileLoanChargeTaxDetails;
	}

	public void setFinabileLoanChargeTaxDetails(Set<FinabileLoanChargeTaxDetails> finabileLoanChargeTaxDetails) {
		this.finabileLoanChargeTaxDetails = finabileLoanChargeTaxDetails;
	}
    
    @OneToOne(mappedBy = "loanCharge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private LoanChargeAdditionalDetails loanChargeAdditionalDetails;

	public LoanChargeAdditionalDetails getLoanChargeAdditionalDetails() {
		return loanChargeAdditionalDetails;
	}

	public void setLoanChargeAdditionalDetails(LoanChargeAdditionalDetails loanChargeAdditionalDetails) {
		this.loanChargeAdditionalDetails = loanChargeAdditionalDetails;
	}
	
	@Transient
	private Boolean applied = false;

	public Boolean getApplied() {
		return applied;
	}

	public void setApplied(Boolean applied) {
		this.applied = applied;
	}
	
	@Transient
	private static ChargeAdditionalDetails chargeAdditionalDetailsBean;


    public static LoanCharge createNewFromJson(final Loan loan, final Charge chargeDefinition, final JsonCommand command,
			/* Habile changes */final LoanChargeAdditionalDetails loanChargeAdditionalDetails) {
        final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
        return createNewFromJson(loan, chargeDefinition, command, dueDate,
				/* Habile changes */loanChargeAdditionalDetails);
    }
    
    public static LoanCharge createNewFromJson(final Loan loan, final Charge chargeDefinition, final LoanTransactionData transationData, final JsonCommand command,final LoanChargeAdditionalDetails loanChargeAdditionalDetails) {
        final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
        return createNewFromJson(loan, chargeDefinition,transationData,command, dueDate, loanChargeAdditionalDetails);
    }
//    /** apply Loan charge on outstanding amount **
    
    public static LoanCharge createNewFromJson(final Loan loan, final Charge chargeDefinition, final LoanTransactionData transationData, final JsonCommand command,
            final LocalDate dueDate,
			/* Habile changes */final LoanChargeAdditionalDetails loanChargeAdditionalDetails) {
        final BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount");

        final ChargeTimeType chargeTime = null;
        final ChargeCalculationType chargeCalculation = null;
        final ChargePaymentMode chargePaymentMode = null;
        BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;
        switch (ChargeCalculationType.fromInt(chargeDefinition.getChargeCalculation())) {
            case PERCENT_OF_OUTSTANDING:
                amountPercentageAppliedTo = transationData.getAmount();
                
            break;          
            default:
            break;
        }

        BigDecimal loanCharge = BigDecimal.ZERO;
               
        
        return new LoanCharge(loan, chargeDefinition, amountPercentageAppliedTo, amount, chargeTime, chargeCalculation, dueDate,
                chargePaymentMode, null, loanCharge,loanChargeAdditionalDetails);
    }
    
    
    public static LoanCharge createNewFromJson(final Loan loan, final Charge chargeDefinition, final JsonCommand command,
            final LocalDate dueDate,
			/* Habile changes */final LoanChargeAdditionalDetails loanChargeAdditionalDetails) {
        final BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount");

        final ChargeTimeType chargeTime = null;
        final ChargeCalculationType chargeCalculation = null;
        final ChargePaymentMode chargePaymentMode = null;
        BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;
        switch (ChargeCalculationType.fromInt(chargeDefinition.getChargeCalculation())) {
            case PERCENT_OF_AMOUNT:
                if (command.hasParameter("principal")) {
                    amountPercentageAppliedTo = command.bigDecimalValueOfParameterNamed("principal");
                } else {
                    amountPercentageAppliedTo = loan.getPrincpal().getAmount();
                }
            break;
            case PERCENT_OF_AMOUNT_AND_INTEREST:
                if (command.hasParameter("principal") && command.hasParameter("interest")) {
                    amountPercentageAppliedTo = command.bigDecimalValueOfParameterNamed("principal").add(
                            command.bigDecimalValueOfParameterNamed("interest"));
                } else {
                    amountPercentageAppliedTo = loan.getPrincpal().getAmount().add(loan.getTotalInterest());
                }
            break;
            case PERCENT_OF_INTEREST:
                if (command.hasParameter("interest")) {
                    amountPercentageAppliedTo = command.bigDecimalValueOfParameterNamed("interest");
                } else {
                    amountPercentageAppliedTo = loan.getTotalInterest();
                }
                      	 
            break;
            default:
            break;
        }

        BigDecimal loanCharge = BigDecimal.ZERO;
        if (ChargeTimeType.fromInt(chargeDefinition.getChargeTimeType()).equals(ChargeTimeType.INSTALMENT_FEE)) {
            BigDecimal percentage = amount;
            if (percentage == null) {
                percentage = chargeDefinition.getAmount();
            }
            loanCharge = loan.calculatePerInstallmentChargeAmount(ChargeCalculationType.fromInt(chargeDefinition.getChargeCalculation()),
                    percentage);
        }

        // If charge type is specified due date and loan is multi disburment
        // loan.
        // Then we need to get as of this loan charge due date how much amount
        // disbursed.
        if (chargeDefinition.getChargeTimeType().equals(ChargeTimeType.SPECIFIED_DUE_DATE.getValue()) && loan.isMultiDisburmentLoan()) {
            amountPercentageAppliedTo = BigDecimal.ZERO;
            for (final LoanDisbursementDetails loanDisbursementDetails : loan.getDisbursementDetails()) {
                if (!loanDisbursementDetails.expectedDisbursementDate().after(dueDate.toDate())) {
                    amountPercentageAppliedTo = amountPercentageAppliedTo.add(loanDisbursementDetails.principal());
                }
            }
        }
        
        return new LoanCharge(loan, chargeDefinition, amountPercentageAppliedTo, amount, chargeTime, chargeCalculation, dueDate,
                chargePaymentMode, null, loanCharge, /* Habile changes */loanChargeAdditionalDetails);
    }

    /*
     * loanPrincipal is required for charges that are percentage based
     */
    public static LoanCharge createNewWithoutLoan(final Charge chargeDefinition, final BigDecimal loanPrincipal, final BigDecimal amount,
            final ChargeTimeType chargeTime, final ChargeCalculationType chargeCalculation, final LocalDate dueDate,
            final ChargePaymentMode chargePaymentMode, final Integer numberOfRepayments,
			/* Habile changes start */final LoanChargeAdditionalDetails loanChargeAdditionalDetails/*
			 * Habile changes
			 * end
			 */) {
        return new LoanCharge(null, chargeDefinition, loanPrincipal, amount, chargeTime, chargeCalculation, dueDate, chargePaymentMode,
                numberOfRepayments, BigDecimal.ZERO,
				/* Habile changes start */loanChargeAdditionalDetails/* Habile changes end */);
    }

    protected LoanCharge() {
        //
    }

    public LoanCharge(final Loan loan, final Charge chargeDefinition, final BigDecimal loanPrincipal, final BigDecimal amount,
            final ChargeTimeType chargeTime, final ChargeCalculationType chargeCalculation, final LocalDate dueDate,
            final ChargePaymentMode chargePaymentMode, final Integer numberOfRepayments, final BigDecimal loanCharge,
			/* Habile changes start */final LoanChargeAdditionalDetails loanChargeAdditionalDetails/*
			 * Habile changes
			 * end
			 */) {
        this.loan = loan;
        this.charge = chargeDefinition;
        this.penaltyCharge = chargeDefinition.isPenalty();
        this.minCap = chargeDefinition.getMinCap();
        this.maxCap = chargeDefinition.getMaxCap();

        this.chargeTime = chargeDefinition.getChargeTimeType();
        if (chargeTime != null) {
            this.chargeTime = chargeTime.getValue();
        }
        if (loanChargeAdditionalDetails != null) {
			loanChargeAdditionalDetails.setLoanCharge(this);
		}
		this.loanChargeAdditionalDetails = loanChargeAdditionalDetails;
        if (ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.SPECIFIED_DUE_DATE)
                || ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.OVERDUE_INSTALLMENT) || 
                ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.PREPAYMENT)) {

            if (dueDate == null) {
                final String defaultUserMessage = "Loan charge is missing due date.";
                throw new LoanChargeWithoutMandatoryFieldException("loanCharge", "dueDate", defaultUserMessage, chargeDefinition.getId(),
                        chargeDefinition.getName());
            }

            this.dueDate = dueDate.toDate();
        } else {
            this.dueDate = null;
        }

        this.chargeCalculation = chargeDefinition.getChargeCalculation();
        if (chargeCalculation != null) {
            this.chargeCalculation = chargeCalculation.getValue();
        }

        BigDecimal chargeAmount = chargeDefinition.getAmount();
        if (amount != null) {
            chargeAmount = amount;
        }

        this.chargePaymentMode = chargeDefinition.getChargePaymentMode();
        if (chargePaymentMode != null) {
            this.chargePaymentMode = chargePaymentMode.getValue();
        }
        populateDerivedFields(loanPrincipal, chargeAmount, numberOfRepayments, loanCharge);
        this.paid = determineIfFullyPaid();
    }

    private void populateDerivedFields(BigDecimal amountPercentageAppliedTo, final BigDecimal chargeAmount,
            Integer numberOfRepayments, BigDecimal loanCharge) {

        switch (ChargeCalculationType.fromInt(this.chargeCalculation)) {
            case INVALID:
                this.percentage = null;
                this.amount = null;
                this.amountPercentageAppliedTo = null;
                this.amountPaid = null;
                this.amountOutstanding = BigDecimal.ZERO;
                this.amountWaived = null;
                this.amountWrittenOff = null;
            break;
            case FLAT:
                this.percentage = null;
                this.amountPercentageAppliedTo = null;
                this.amountPaid = null;
                if (isInstalmentFee()) {
                    if (numberOfRepayments == null) {
                        numberOfRepayments = this.loan.fetchNumberOfInstallmensAfterExceptions();
                    }
                    this.amount = chargeAmount.multiply(BigDecimal.valueOf(numberOfRepayments));
                } else {
                    this.amount = chargeAmount;
                }
                this.amountOutstanding = this.amount;
                this.amountWaived = null;
                this.amountWrittenOff = null;
            break;
            case PERCENT_OF_AMOUNT:
            case PERCENT_OF_AMOUNT_AND_INTEREST:
            case PERCENT_OF_INTEREST:
            case PERCENT_OF_DISBURSEMENT_AMOUNT:
            case PERCENT_OF_OUTSTANDING:
            	
                this.percentage = chargeAmount;
                this.amountPercentageAppliedTo = amountPercentageAppliedTo;
                if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
                    loanCharge = percentageOf(this.amountPercentageAppliedTo);
                    /** Habile changes start enable pay day loan calculations */
    				if (this.loanChargeAdditionalDetails != null
    						&& this.loanChargeAdditionalDetails.getIsEnabledFeeCalculationBasedOnTenure() == 1) {
    					Loan loanDetails = getLoan();
    					if (loanDetails != null) {
    						BigDecimal tenure = BigDecimal.ZERO;
    						if (loanChargeAdditionalDetails.getNumberOfDays() == null) {
    							LocalDate disbursementDate = loanDetails.getExpectedDisbursedOnLocalDate();
    							LocalDate maturityDate = loanDetails.getExpectedMaturityDate();
    							Integer noOfDays = Days.daysBetween(disbursementDate, maturityDate).getDays();
    							loanChargeAdditionalDetails.setNumberOfDays(noOfDays);
    							loanCharge = loanCharge
    									.multiply(BigDecimal.valueOf(loanChargeAdditionalDetails.getNumberOfDays()));
    						} else {
    							loanCharge = loanCharge
    									.multiply(BigDecimal.valueOf(loanChargeAdditionalDetails.getNumberOfDays()));
    						}
    					}
    				}
    				/** Habile changes end */
                }
                this.amount = minimumAndMaximumCap(loanCharge);
                this.amountPaid = null;
                this.amountOutstanding = calculateOutstanding();
                this.amountWaived = null;
                this.amountWrittenOff = null;
            break;
    		/** Habile changes for extension fees calculation */
    		case PERCENT_OF_OUTSTANDING_LOAN_AMOUNT:

    			this.percentage = chargeAmount;
    			if (this.amountPercentageAppliedTo == null || this.amountPercentageAppliedTo.longValue() == 0) {
    				if (loan.getTotalOverpaid() != null && loan.getTotalOverpaid().longValue() > 0) {
    					amountPercentageAppliedTo = loan.getSummary().getTotalPrincipalOutstanding()
    							.subtract(loan.getTotalOverpaid());
    				} else {
    					amountPercentageAppliedTo = loan.getSummary().getTotalPrincipalOutstanding();
    				}
    				this.amountPercentageAppliedTo = amountPercentageAppliedTo;
    			}
    			if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
    				Integer numberOfDays = 0;
    				if (this.loanChargeAdditionalDetails != null
    						&& this.loanChargeAdditionalDetails.getIsEnabledFeeCalculationBasedOnTenure() == 1) {
    					numberOfDays = this.loanChargeAdditionalDetails.getNumberOfDays();
    					if (numberOfDays != null && numberOfDays > 0) {
    						loanCharge = percentageOf(this.amountPercentageAppliedTo, this.percentage, numberOfDays);
    					}
    				} else {
    					loanCharge = percentageOf(this.amountPercentageAppliedTo);
    				}

    			}
    			this.amount = minimumAndMaximumCap(loanCharge);
    			if (this.loanChargeAdditionalDetails != null) {
    				if (this.loanChargeAdditionalDetails.getIsEnabledAutoPaid() == 1) {
    					this.amountPaid = this.amount;
    				}
    			}
    			this.amountOutstanding = calculateOutstanding();
    			/*
    			 * this.percentage = chargeAmount; this.amountPercentageAppliedTo =
    			 * BigDecimal.ZERO; if (loanCharge.compareTo(BigDecimal.ZERO) == 0) { loanCharge
    			 * = percentageOf(this.amountPercentageAppliedTo); } this.amount =
    			 * minimumAndMaximumCap(loanCharge); this.amountPaid = null;
    			 * this.amountOutstanding = calculateOutstanding(); this.amountWaived = null;
    			 * this.amountWrittenOff = null;
    			 */
    			break;

    		/*case SLAB:
    			this.percentage = null;
    			this.amountPercentageAppliedTo = null;
    			this.amountPaid = null;
    			if (isInstalmentFee()) {
    				if (numberOfRepayments == null) {
    					numberOfRepayments = this.loan.fetchNumberOfInstallmensAfterExceptions();
    				}
    				this.amount = chargeAmount.multiply(BigDecimal.valueOf(numberOfRepayments));
    			} else {
    				this.amount = chargeAmount;
    			}
    			this.amountOutstanding = this.amount;
    			this.amountWaived = null;
    			this.amountWrittenOff = null;
    			break;*/
        }
        this.amountOrPercentage = chargeAmount;
        if (this.loan != null && isInstalmentFee()) {
            updateInstallmentCharges();
        }
        /** Habile changes for tax */
		BigDecimal taxComponent = BigDecimal.ZERO;
		BigDecimal feesComponent = BigDecimal.ZERO;
		if (this.charge.getTaxGroup() != null && this.loanChargeAdditionalDetails != null) {
			Iterator<TaxGroupMappings> taxGroupMappings = this.charge.getTaxGroup().getTaxGroupMappings().iterator();
			Set<FinabileLoanChargeTaxDetails> taxComponentDetails = new HashSet<>();

			BigDecimal totalTaxComponentPercentage = BigDecimal.ZERO;
			if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 1) {
				while (taxGroupMappings.hasNext()) {
					TaxGroupMappings taxGroupMapping = taxGroupMappings.next();
					if (taxGroupMapping.getTaxComponent().getTaxComponentAdditionalDetails().getTaxComponentType()
							.equalsIgnoreCase(this.loanChargeAdditionalDetails.getTaxComponentType())) {
						totalTaxComponentPercentage = totalTaxComponentPercentage
								.add(taxGroupMapping.getTaxComponent().getPercentage());
					}
				}
				feesComponent = this.amount.divide(
						BigDecimal.ONE.add(totalTaxComponentPercentage.divide(new BigDecimal(100))), 2,
						RoundingMode.HALF_EVEN);
			}

			Iterator<TaxGroupMappings> taxMappings = this.charge.getTaxGroup().getTaxGroupMappings().iterator();
			while (taxMappings.hasNext()) {
				TaxGroupMappings taxGroupMapping = taxMappings.next();
				if (taxGroupMapping.getTaxComponent().getTaxComponentAdditionalDetails().getTaxComponentType()
						.equalsIgnoreCase(this.loanChargeAdditionalDetails.getTaxComponentType())) {

					BigDecimal taxComponentPercentage = taxGroupMapping.getTaxComponent().getPercentage();
					BigDecimal taxAmount = BigDecimal.ZERO;
					if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 1) {
						taxAmount = feesComponent.multiply(taxComponentPercentage).divide(new BigDecimal(100))
								.setScale(0, RoundingMode.HALF_EVEN);
					} else {
						taxAmount = this.amount.multiply(taxComponentPercentage).divide(new BigDecimal(100)).setScale(0,
								RoundingMode.HALF_EVEN);
					}

					taxComponent = taxComponent.add(taxAmount).setScale(0, RoundingMode.HALF_EVEN);
					FinabileLoanChargeTaxDetails finabileLoanChargeTaxDetails = new FinabileLoanChargeTaxDetails(this,
							taxGroupMapping.getTaxComponent(), taxGroupMapping.getTaxComponent().getName(), taxAmount);
					taxComponentDetails.add(finabileLoanChargeTaxDetails);
				}
			}

			/*
			 * if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 1) { feesComponent
			 * = this.amount.subtract(taxComponent); //this.amount =
			 * minimumAndMaximumCap(loanCharge); } else
			 */ if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 0) {
				feesComponent = this.amount;
				this.amount = minimumAndMaximumCap(this.amount.add(taxComponent));
			}
			this.finabileLoanChargeTaxDetails = taxComponentDetails;
			this.loanChargeAdditionalDetails.setFeesComponent(feesComponent.setScale(0, RoundingMode.HALF_EVEN));
			this.loanChargeAdditionalDetails.setTaxComponent(taxComponent.setScale(0, RoundingMode.HALF_EVEN));
			if (this.loanChargeAdditionalDetails != null) {
				if (this.loanChargeAdditionalDetails.getIsEnabledAutoPaid() == 1) {
					this.amountPaid = this.amount;
				}
			}
			this.amountOutstanding = calculateOutstanding();
			this.amountWaived = null;
			this.amountWrittenOff = null;
		}
		/** Habile changes end */
    }

    public void markAsFullyPaid() {
        this.amountPaid = this.amount;
        this.amountOutstanding = BigDecimal.ZERO;
        this.paid = true;
    }

    public boolean isFullyPaid() {
        return this.paid;
    }

    public void resetToOriginal(final MonetaryCurrency currency) {
        this.amountPaid = BigDecimal.ZERO;
        this.amountWaived = BigDecimal.ZERO;
        this.amountWrittenOff = BigDecimal.ZERO;
        this.amountOutstanding = calculateAmountOutstanding(currency);
        this.paid = false;
        this.waived = false;
        for (final LoanInstallmentCharge installmentCharge : this.loanInstallmentCharge) {
            installmentCharge.resetToOriginal(currency);
        }
    }

    public void resetPaidAmount(final MonetaryCurrency currency) {
        this.amountPaid = BigDecimal.ZERO;
        this.amountOutstanding = calculateAmountOutstanding(currency);
        this.paid = false;
        for (final LoanInstallmentCharge installmentCharge : this.loanInstallmentCharge) {
            installmentCharge.resetPaidAmount(currency);
        }
    }

    public Money waive(final MonetaryCurrency currency, final Integer loanInstallmentNumber) {
        if (isInstalmentFee()) {
            final LoanInstallmentCharge chargePerInstallment = getInstallmentLoanCharge(loanInstallmentNumber);
            final Money amountWaived = chargePerInstallment.waive(currency);
            if (this.amountWaived == null) {
                this.amountWaived = BigDecimal.ZERO;
            }
            this.amountWaived = this.amountWaived.add(amountWaived.getAmount());
            this.amountOutstanding = this.amountOutstanding.subtract(amountWaived.getAmount());
            if (determineIfFullyPaid()) {
                this.paid = false;
                this.waived = true;
            }
            return amountWaived;
        }
        this.amountWaived = this.amountOutstanding;
        this.amountOutstanding = BigDecimal.ZERO;
        this.paid = false;
        this.waived = true;
        return getAmountWaived(currency);

    }

    public BigDecimal getAmountPercentageAppliedTo() {
        return this.amountPercentageAppliedTo;
    }

    private BigDecimal calculateAmountOutstanding(final MonetaryCurrency currency) {
        return getAmount(currency).minus(getAmountWaived(currency)).minus(getAmountPaid(currency)).getAmount();
    }

    public void update(final Loan loan) {
        this.loan = loan;
    }

    public void update(final BigDecimal amount, final LocalDate dueDate, final BigDecimal loanPrincipal, Integer numberOfRepayments,
            BigDecimal loanCharge) {
        if (dueDate != null) {
            this.dueDate = dueDate.toDate();
        }

        if (amount != null) {
            switch (ChargeCalculationType.fromInt(this.chargeCalculation)) {
                case INVALID:
                break;
                case FLAT:
                    if (isInstalmentFee()) {
                        if (numberOfRepayments == null) {
                            numberOfRepayments = this.loan.fetchNumberOfInstallmensAfterExceptions();
                        }
                        this.amount = amount.multiply(BigDecimal.valueOf(numberOfRepayments));
                    } else {
                        this.amount = amount;
                    }
                break;
                case PERCENT_OF_AMOUNT:
                case PERCENT_OF_AMOUNT_AND_INTEREST:
                case PERCENT_OF_INTEREST:
                case PERCENT_OF_DISBURSEMENT_AMOUNT:
                    this.percentage = amount;
                    this.amountPercentageAppliedTo = loanPrincipal;
                    if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
                        loanCharge = percentageOf(this.amountPercentageAppliedTo);
                    }
                    this.amount = minimumAndMaximumCap(loanCharge);
                    /** Habile changes charge calculation based on tenure */
    				if (this.loanChargeAdditionalDetails != null
    						&& this.loanChargeAdditionalDetails.getIsEnabledFeeCalculationBasedOnTenure() == 1) {
    					Loan loanDetails = getLoan();
    					if (loanDetails != null) {
    						if (loanChargeAdditionalDetails.getNumberOfDays() == null) {
    							LocalDate disbursementDate = loanDetails.getExpectedDisbursedOnLocalDate();
    							LocalDate maturityDate = loanDetails.getExpectedMaturityDate();
    							Integer noOfDays = Days.daysBetween(disbursementDate, maturityDate).getDays();
    							loanChargeAdditionalDetails.setNumberOfDays(noOfDays);
    							loanCharge = loanCharge
    									.multiply(BigDecimal.valueOf(loanChargeAdditionalDetails.getNumberOfDays()));
    						} else {
    							loanCharge = loanCharge
    									.multiply(BigDecimal.valueOf(loanChargeAdditionalDetails.getNumberOfDays()));
    						}

    					}
    				}
    				/** Habile changes end */
                break;
            }
            this.amountOrPercentage = amount;
            this.amountOutstanding = calculateOutstanding();
            if (this.loan != null && isInstalmentFee()) {
                updateInstallmentCharges();
            }
            /** Habile changes for tax */
			BigDecimal taxComponent = BigDecimal.ZERO;
			BigDecimal feesComponent = BigDecimal.ZERO;
			if (this.charge.getTaxGroup() != null && this.loanChargeAdditionalDetails != null) {
				Iterator<TaxGroupMappings> taxGroupMappings = this.charge.getTaxGroup().getTaxGroupMappings()
						.iterator();
				Set<FinabileLoanChargeTaxDetails> taxComponentDetails = new HashSet<>();

				BigDecimal totalTaxComponentPercentage = BigDecimal.ZERO;
				if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 1) {
					while (taxGroupMappings.hasNext()) {
						TaxGroupMappings taxGroupMapping = taxGroupMappings.next();
						if (taxGroupMapping.getTaxComponent().getTaxComponentAdditionalDetails().getTaxComponentType()
								.equalsIgnoreCase(this.loanChargeAdditionalDetails.getTaxComponentType())) {
							totalTaxComponentPercentage = totalTaxComponentPercentage
									.add(taxGroupMapping.getTaxComponent().getPercentage());
						}
					}
					feesComponent = this.amount.divide(
							BigDecimal.ONE.add(totalTaxComponentPercentage.divide(new BigDecimal(100))), 2,
							RoundingMode.HALF_EVEN);
				}

				Iterator<TaxGroupMappings> taxMappings = this.charge.getTaxGroup().getTaxGroupMappings().iterator();
				while (taxMappings.hasNext()) {
					TaxGroupMappings taxGroupMapping = taxMappings.next();
					if (taxGroupMapping.getTaxComponent().getTaxComponentAdditionalDetails().getTaxComponentType()
							.equalsIgnoreCase(this.loanChargeAdditionalDetails.getTaxComponentType())) {

						BigDecimal taxComponentPercentage = taxGroupMapping.getTaxComponent().getPercentage();
						// BigDecimal taxAmount =
						// this.amount.multiply(taxComponentPercentage).divide(new
						// BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN);
						BigDecimal taxAmount = BigDecimal.ZERO;
						if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 1) {
							taxAmount = feesComponent.multiply(taxComponentPercentage).divide(new BigDecimal(100))
									.setScale(0, RoundingMode.HALF_EVEN);
						} else {
							taxAmount = this.amount.multiply(taxComponentPercentage).divide(new BigDecimal(100))
									.setScale(0, RoundingMode.HALF_EVEN);
						}
						taxComponent = taxComponent.add(taxAmount).setScale(0, RoundingMode.HALF_EVEN);
						FinabileLoanChargeTaxDetails finabileLoanChargeTaxDetails = new FinabileLoanChargeTaxDetails(
								this, taxGroupMapping.getTaxComponent(), taxGroupMapping.getTaxComponent().getName(),
								taxAmount);
						taxComponentDetails.add(finabileLoanChargeTaxDetails);
					}
				}

				if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 1) {
					if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 1) {
						if(!(this.amount.subtract(taxComponent).doubleValue() == feesComponent.doubleValue())) {
							feesComponent = this.amount.subtract(taxComponent);
						}
					}
				} else if (this.loanChargeAdditionalDetails.getIsTaxIncluded() == 0) {
					feesComponent = this.amount;
					this.amount = minimumAndMaximumCap(this.amount.add(taxComponent));
				}

				this.finabileLoanChargeTaxDetails = taxComponentDetails;
				this.loanChargeAdditionalDetails.setFeesComponent(feesComponent.setScale(0, RoundingMode.HALF_EVEN));
				this.loanChargeAdditionalDetails.setTaxComponent(taxComponent.setScale(0, RoundingMode.HALF_EVEN));
				if (this.loan != null) {
					this.loanChargeAdditionalDetails
							.setFeesComponent(Money.of(this.loan.getCurrency(), feesComponent).getAmount());
					this.loanChargeAdditionalDetails
							.setTaxComponent(Money.of(this.loan.getCurrency(), taxComponent).getAmount());
				}
				if (this.loanChargeAdditionalDetails != null) {
					if (this.loanChargeAdditionalDetails.getIsEnabledAutoPaid() == 1) {
						this.amountPaid = this.amount;
					}
				}
				this.amountOutstanding = calculateOutstanding();
				this.amountWaived = null;
				this.amountWrittenOff = null;
			}
			/** Habile changes end */
        }
    }
    /** Habile changes for extension fee calculation */
	public static LoanCharge createNewFromJson(final LocalDate dueDate, final Loan loan, final Charge chargeDefinition,
			final LoanChargeAdditionalDetails loanChargeAdditionalDetails) {

		final BigDecimal amount = null;

		final ChargeTimeType chargeTime = null;
		final ChargeCalculationType chargeCalculation = null;
		final ChargePaymentMode chargePaymentMode = null;

		BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;
		BigDecimal loanCharge = BigDecimal.ZERO;

		return new LoanCharge(loan, chargeDefinition, amountPercentageAppliedTo, amount, chargeTime, chargeCalculation,
				dueDate, chargePaymentMode, null, loanCharge,
				/* Habile changes start */loanChargeAdditionalDetails /* Habile changes start */);
	}

	public static BigDecimal percentageOf(final BigDecimal value, final BigDecimal percentage, final int numberOfDays) {

		BigDecimal percentageOf = BigDecimal.ZERO;

		if (isGreaterThanZero(value)) {
			final MathContext mc = new MathContext(8, MoneyHelper.getRoundingMode());
			final BigDecimal multiplicand = percentage.divide(BigDecimal.valueOf(100l), mc);
			percentageOf = value.multiply(multiplicand.multiply(BigDecimal.valueOf(numberOfDays), mc), mc);
		}
		return percentageOf;
	}
    
    public boolean isInstallmentRescheduled() {
		return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.INSTALLMENT_RESCHEDULED);
	}

    public void update(final BigDecimal amount, final LocalDate dueDate, final Integer numberOfRepayments) {
        BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;
        if (this.loan != null) {
            switch (ChargeCalculationType.fromInt(this.chargeCalculation)) {
                case PERCENT_OF_AMOUNT:
                    // If charge type is specified due date and loan is multi
                    // disburment loan.
                    // Then we need to get as of this loan charge due date how
                    // much amount disbursed.
                    if (this.loan.isMultiDisburmentLoan() && this.isSpecifiedDueDate()) {
                        for (final LoanDisbursementDetails loanDisbursementDetails : this.loan.getDisbursementDetails()) {
                            if (!loanDisbursementDetails.expectedDisbursementDate().after(this.getDueDate())) {
                                amountPercentageAppliedTo = amountPercentageAppliedTo.add(loanDisbursementDetails.principal());
                            }
                        }
                    } else {
                        amountPercentageAppliedTo = this.loan.getPrincpal().getAmount();
                    }
                break;
                case PERCENT_OF_AMOUNT_AND_INTEREST:
                    amountPercentageAppliedTo = this.loan.getPrincpal().getAmount().add(this.loan.getTotalInterest());
                break;
                case PERCENT_OF_INTEREST:
                    amountPercentageAppliedTo = this.loan.getTotalInterest();
                break;
                case PERCENT_OF_DISBURSEMENT_AMOUNT:
                    LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = this.loanTrancheDisbursementCharge;
                    amountPercentageAppliedTo = loanTrancheDisbursementCharge.getloanDisbursementDetails().principal();
                break;
                default:
                break;
            }
        }
        update(amount, dueDate, amountPercentageAppliedTo, numberOfRepayments, BigDecimal.ZERO);
    }

    public Map<String, Object> update(final JsonCommand command, final BigDecimal amount) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        final String dueDateParamName = "dueDate";
        if (command.isChangeInLocalDateParameterNamed(dueDateParamName, getDueLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(dueDateParamName);
            actualChanges.put(dueDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(dueDateParamName);
            this.dueDate = newValue.toDate();
        }

        final String amountParamName = "amount";
        if (command.isChangeInBigDecimalParameterNamed(amountParamName, this.amount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(amountParamName);
            BigDecimal loanCharge = null;
            actualChanges.put(amountParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            switch (ChargeCalculationType.fromInt(this.chargeCalculation)) {
                case INVALID:
                break;
                case FLAT:
                    if (isInstalmentFee()) {
                        this.amount = newValue.multiply(BigDecimal.valueOf(this.loan.fetchNumberOfInstallmensAfterExceptions()));
                    } else {
                        this.amount = newValue;
                    }
                    this.amountOutstanding = calculateOutstanding();
                break;
                case PERCENT_OF_AMOUNT:
                case PERCENT_OF_AMOUNT_AND_INTEREST:
                case PERCENT_OF_INTEREST:
                case PERCENT_OF_DISBURSEMENT_AMOUNT:
                    this.percentage = newValue;
                    this.amountPercentageAppliedTo = amount;
                    loanCharge = BigDecimal.ZERO;
                    if (isInstalmentFee()) {
                        loanCharge = this.loan.calculatePerInstallmentChargeAmount(ChargeCalculationType.fromInt(this.chargeCalculation),
                                this.percentage);
                    }
                    if (loanCharge.compareTo(BigDecimal.ZERO) == 0) {
                        loanCharge = percentageOf(this.amountPercentageAppliedTo);
                    }
                    /** Habile changes to calculate charge based on loan tenure */
					if (this.loanChargeAdditionalDetails != null
							&& this.loanChargeAdditionalDetails.getNumberOfDays() != null) {
						if (this.loanChargeAdditionalDetails.getNumberOfDays() > 0) {
							loanCharge = loanCharge
									.multiply(BigDecimal.valueOf(this.loanChargeAdditionalDetails.getNumberOfDays()));
						}
					}
					/** Habile changes end */
                    
                    this.amount = minimumAndMaximumCap(loanCharge);
                    this.amountOutstanding = calculateOutstanding();
                break;
            }
            this.amountOrPercentage = newValue;
            if (isInstalmentFee()) {
                updateInstallmentCharges();
            }
        }
        return actualChanges;
    }

    private void updateInstallmentCharges() {
        final Collection<LoanInstallmentCharge> remove = new HashSet<>();
        final List<LoanInstallmentCharge> newChargeInstallments = this.loan.generateInstallmentLoanCharges(this);
        if (this.loanInstallmentCharge.isEmpty()) {
            this.loanInstallmentCharge.addAll(newChargeInstallments);
        } else {
            int index = 0;
            final List<LoanInstallmentCharge> oldChargeInstallments = new ArrayList<>();
            if(this.loanInstallmentCharge != null && !this.loanInstallmentCharge.isEmpty()){
                oldChargeInstallments.addAll(this.loanInstallmentCharge);
            }
            Collections.sort(oldChargeInstallments);
            final LoanInstallmentCharge[] loanChargePerInstallmentArray = newChargeInstallments.toArray(new LoanInstallmentCharge[newChargeInstallments.size()]);
            for (final LoanInstallmentCharge chargePerInstallment : oldChargeInstallments) {
                if (index == loanChargePerInstallmentArray.length) {
                    remove.add(chargePerInstallment);
                    chargePerInstallment.updateInstallment(null);
                } else {
                    chargePerInstallment.copyFrom(loanChargePerInstallmentArray[index++]);
                }
            }
            this.loanInstallmentCharge.removeAll(remove);
            while (index < loanChargePerInstallmentArray.length) {
                this.loanInstallmentCharge.add(loanChargePerInstallmentArray[index++]);
            }
        }
        Money amount = Money.zero(this.loan.getCurrency());
        for(LoanInstallmentCharge charge:this.loanInstallmentCharge){
            amount =amount.plus(charge.getAmount());
        }
        this.amount =amount.getAmount();
    }

    public boolean isDueAtDisbursement() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.DISBURSEMENT)
                || ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.TRANCHE_DISBURSEMENT);
    }

    public boolean isSpecifiedDueDate() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.SPECIFIED_DUE_DATE);
    }
    
    public boolean isPrePayment() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.PREPAYMENT);
    }

    public boolean isInstalmentFee() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.INSTALMENT_FEE);
    }

    public boolean isOverdueInstallmentCharge() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.OVERDUE_INSTALLMENT);
    }

    private static boolean isGreaterThanZero(final BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) == 1;
    }

    public LoanChargeCommand toCommand() {
        return new LoanChargeCommand(getId(), this.charge.getId(), this.amount, this.chargeTime, this.chargeCalculation, getDueLocalDate());
    }

    public LocalDate getDueLocalDate() {
        LocalDate dueDate = null;
        if (this.dueDate != null) {
            dueDate = new LocalDate(this.dueDate);
        }
        return dueDate;
    }
    
    public Date getDueDate() {
        return this.dueDate;
    }

    private boolean determineIfFullyPaid() {
        if (this.amount == null) { return true; }
        return BigDecimal.ZERO.compareTo(calculateOutstanding()) == 0;
    }

    private BigDecimal calculateOutstanding() {
        if (this.amount == null) { return null; }
        BigDecimal amountPaidLocal = BigDecimal.ZERO;
        if (this.amountPaid != null) {
            amountPaidLocal = this.amountPaid;
        }

        BigDecimal amountWaivedLocal = BigDecimal.ZERO;
        if (this.amountWaived != null) {
            amountWaivedLocal = this.amountWaived;
        }

        BigDecimal amountWrittenOffLocal = BigDecimal.ZERO;
        if (this.amountWrittenOff != null) {
            amountWrittenOffLocal = this.amountWrittenOff;
        }

        final BigDecimal totalAccountedFor = amountPaidLocal.add(amountWaivedLocal).add(amountWrittenOffLocal);

        return this.amount.subtract(totalAccountedFor);
    }

    public BigDecimal percentageOf(final BigDecimal value) {
        return percentageOf(value, this.percentage);
    }

    public static BigDecimal percentageOf(final BigDecimal value, final BigDecimal percentage) {

        BigDecimal percentageOf = BigDecimal.ZERO;

        if (isGreaterThanZero(value)) {
            final MathContext mc = new MathContext(8, MoneyHelper.getRoundingMode());
            final BigDecimal multiplicand = percentage.divide(BigDecimal.valueOf(100l), mc);
            percentageOf = value.multiply(multiplicand, mc);
        }
        return percentageOf;
    }

    /**
     * @param percentageOf
     * @returns a minimum cap or maximum cap set on charges if the criteria fits
     *          else it returns the percentageOf if the amount is within min and
     *          max cap
     */
    private BigDecimal minimumAndMaximumCap(final BigDecimal percentageOf) {
        BigDecimal minMaxCap = BigDecimal.ZERO;
        if (this.minCap != null) {
            final int minimumCap = percentageOf.compareTo(this.minCap);
            if (minimumCap == -1) {
                minMaxCap = this.minCap;
                return minMaxCap;
            }
        }
        if (this.maxCap != null) {
            final int maximumCap = percentageOf.compareTo(this.maxCap);
            if (maximumCap == 1) {
                minMaxCap = this.maxCap;
                return minMaxCap;
            }
        }
        minMaxCap = percentageOf;
        // this will round the amount value
        if (this.loan != null && minMaxCap != null) {
            minMaxCap = Money.of(this.loan.getCurrency(), minMaxCap).getAmount();
        }
        return minMaxCap;
    }

    public BigDecimal amount() {
        return this.amount;
    }

    public BigDecimal amountOutstanding() {
        return this.amountOutstanding;
    }
    
    public Money getAmountOutstanding(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountOutstanding);
    }

    public boolean hasNotLoanIdentifiedBy(final Long loanId) {
        return !hasLoanIdentifiedBy(loanId);
    }

    public boolean hasLoanIdentifiedBy(final Long loanId) {
        return this.loan.hasIdentifyOf(loanId);
    }

    public boolean isDueForCollectionFromAndUpToAndIncluding(final LocalDate fromNotInclusive, final LocalDate upToAndInclusive) {
        final LocalDate dueDate = getDueLocalDate();
        return occursOnDayFromAndUpToAndIncluding(fromNotInclusive, upToAndInclusive, dueDate);
    }

    private boolean occursOnDayFromAndUpToAndIncluding(final LocalDate fromNotInclusive, final LocalDate upToAndInclusive,
            final LocalDate target) {
        return target != null && target.isAfter(fromNotInclusive) && !target.isAfter(upToAndInclusive);
    }
    
    public boolean isDueForCollectionFromAndUpToAndIncludingDate(final LocalDate fromNotInclusive, final LocalDate upToAndInclusive) {
        final LocalDate dueDate = getDueLocalDate();
        return occursOnDayFromAndUpToAndIncludingDate(fromNotInclusive, upToAndInclusive, dueDate);
    }

    private boolean occursOnDayFromAndUpToAndIncludingDate(final LocalDate fromNotInclusive, final LocalDate upToAndInclusive,
            final LocalDate target) {
        return target != null && (target.isAfter(fromNotInclusive)|| target.isEqual(fromNotInclusive)) && !target.isAfter(upToAndInclusive) && !target.isEqual(upToAndInclusive);
    }

    public boolean isFeeCharge() {
        return !this.penaltyCharge;
    }

    public boolean isPenaltyCharge() {
        return this.penaltyCharge;
    }

    public boolean isNotFullyPaid() {
        return !isPaid();
    }
    
    public boolean isChargePending(){
        return isNotFullyPaid() && !isWaived();
    }

    public boolean isPaid() {
        return this.paid;
    }

    public boolean isWaived() {
        return this.waived;
    }

    public BigDecimal getMinCap() {
        return this.minCap;
    }

    public BigDecimal getMaxCap() {
        return this.maxCap;
    }

    public boolean isPaidOrPartiallyPaid(final MonetaryCurrency currency) {

        final Money amountWaivedOrWrittenOff = getAmountWaived(currency).plus(getAmountWrittenOff(currency));
        return Money.of(currency, this.amountPaid).plus(amountWaivedOrWrittenOff).isGreaterThanZero();
    }

    public Money getAmount(final MonetaryCurrency currency) {
        return Money.of(currency, this.amount);
    }

    public Money getAmountPaid(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountPaid);
    }

    public Money getAmountWaived(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountWaived);
    }

    public Money getAmountWrittenOff(final MonetaryCurrency currency) {
        return Money.of(currency, this.amountWrittenOff);
    }

    /**
     * @param incrementBy
     * 
     * @param installmentNumber
     * 
     * @param feeAmount
     *            TODO
     * 
     * 
     * @return Actual amount paid on this charge
     */
    public Money updatePaidAmountBy(final Money incrementBy, final Integer installmentNumber, final Money feeAmount) {
        Money processAmount = Money.zero(incrementBy.getCurrency());
        if (isInstalmentFee()) {
            if (installmentNumber == null) {
                processAmount = getUnpaidInstallmentLoanCharge().updatePaidAmountBy(incrementBy, feeAmount);
            } else {
                processAmount = getInstallmentLoanCharge(installmentNumber).updatePaidAmountBy(incrementBy, feeAmount);
            }
        } else {
            processAmount = incrementBy;
        }
        Money amountPaidToDate = Money.of(processAmount.getCurrency(), this.amountPaid);
        final Money amountOutstanding = Money.of(processAmount.getCurrency(), this.amountOutstanding);

        Money amountPaidOnThisCharge = Money.zero(processAmount.getCurrency());
        if (processAmount.isGreaterThanOrEqualTo(amountOutstanding)) {
            amountPaidOnThisCharge = amountOutstanding;
            amountPaidToDate = amountPaidToDate.plus(amountOutstanding);
            this.amountPaid = amountPaidToDate.getAmount();
            this.amountOutstanding = BigDecimal.ZERO;
            Money waivedAmount = getAmountWaived(processAmount.getCurrency());
            if (waivedAmount.isGreaterThanZero()) {
                this.waived = true;
            } else {
                this.paid = true;
            }

        } else {
            amountPaidOnThisCharge = processAmount;
            amountPaidToDate = amountPaidToDate.plus(processAmount);
            this.amountPaid = amountPaidToDate.getAmount();
            this.amountOutstanding = calculateAmountOutstanding(incrementBy.getCurrency());
        }
        return amountPaidOnThisCharge;
    }

    public String name() {
        return this.charge.getName();
    }

    public String currencyCode() {
        return this.charge.getCurrencyCode();
    }

    public Charge getCharge() {
        return this.charge;
    }

    /*@Override
    public boolean equals(final Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        final LoanCharge rhs = (LoanCharge) obj;
        return new EqualsBuilder().appendSuper(super.equals(obj)) //
                .append(getId(), rhs.getId()) //
                .append(this.charge.getId(), rhs.charge.getId()) //
                .append(this.amount, rhs.amount) //
                .append(getDueLocalDate(), rhs.getDueLocalDate()) //
                .isEquals();
    }

    @Override
    public int hashCode() {
        return 1;
        
         * return new HashCodeBuilder(3, 5) // .append(getId()) //
         * .append(this.charge.getId()) //
         * .append(this.amount).append(getDueLocalDate()) // .toHashCode();
         
    }*/

    public ChargePaymentMode getChargePaymentMode() {
        return ChargePaymentMode.fromInt(this.chargePaymentMode);
    }

    public ChargeCalculationType getChargeCalculation() {
        return ChargeCalculationType.fromInt(this.chargeCalculation);
    }

    public BigDecimal getPercentage() {
        return this.percentage;
    }

    public void updateAmount(final BigDecimal amount) {
        this.amount = amount;
        calculateOutstanding();
    }

    public LoanInstallmentCharge getUnpaidInstallmentLoanCharge() {
        LoanInstallmentCharge unpaidChargePerInstallment = null;
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (loanChargePerInstallment.isPending()
                    && (unpaidChargePerInstallment == null || unpaidChargePerInstallment.getRepaymentInstallment().getDueDate()
                            .isAfter(loanChargePerInstallment.getRepaymentInstallment().getDueDate()))) {
                unpaidChargePerInstallment = loanChargePerInstallment;
            }
        }
        return unpaidChargePerInstallment;
    }

    public LoanInstallmentCharge getInstallmentLoanCharge(final LocalDate periodDueDate) {
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (periodDueDate.isEqual(loanChargePerInstallment.getRepaymentInstallment().getDueDate())) { return loanChargePerInstallment; }
        }
        return null;
    }

    public LoanInstallmentCharge getInstallmentLoanCharge(final Integer installmentNumber) {
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (installmentNumber.equals(loanChargePerInstallment.getRepaymentInstallment().getInstallmentNumber().intValue())) { return loanChargePerInstallment; }
        }
        return null;
    }

    public void clearLoanInstallmentCharges() {
        this.loanInstallmentCharge.clear();
    }

    public void addLoanInstallmentCharges(final Collection<LoanInstallmentCharge> installmentCharges) {
        this.loanInstallmentCharge.addAll(installmentCharges);
    }

    public boolean hasNoLoanInstallmentCharges() {
        return this.loanInstallmentCharge.isEmpty();
    }

    public Set<LoanInstallmentCharge> installmentCharges() {
        return this.loanInstallmentCharge;
    }

    public List<LoanChargePaidDetail> fetchRepaymentInstallment(final MonetaryCurrency currency) {
        List<LoanChargePaidDetail> chargePaidDetails = new ArrayList<>();
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            if (!loanChargePerInstallment.isChargeAmountpaid(currency)
                    && loanChargePerInstallment.getAmountThroughChargePayment(currency).isGreaterThanZero()) {
                LoanChargePaidDetail chargePaidDetail = new LoanChargePaidDetail(
                        loanChargePerInstallment.getAmountThroughChargePayment(currency),
                        loanChargePerInstallment.getRepaymentInstallment(), isFeeCharge());
                chargePaidDetails.add(chargePaidDetail);
            }
        }
        return chargePaidDetails;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.overdueInstallmentCharge = null;
            this.loanTrancheDisbursementCharge = null;
            this.clearLoanInstallmentCharges();
        }
    }

    public BigDecimal amountOrPercentage() {
        return this.amountOrPercentage;
    }

    public BigDecimal chargeAmount() {
        BigDecimal totalChargeAmount = this.amountOutstanding;
        if (this.amountPaid != null) {
            totalChargeAmount = totalChargeAmount.add(this.amountPaid);
        }
        if (this.amountWaived != null) {
            totalChargeAmount = totalChargeAmount.add(this.amountWaived);
        }
        if (this.amountWrittenOff != null) {
            totalChargeAmount = totalChargeAmount.add(this.amountWrittenOff);
        }
        return totalChargeAmount;
    }

    public void updateOverdueInstallmentCharge(LoanOverdueInstallmentCharge overdueInstallmentCharge) {
        this.overdueInstallmentCharge = overdueInstallmentCharge;
    }

    public void updateLoanTrancheDisbursementCharge(final LoanTrancheDisbursementCharge loanTrancheDisbursementCharge) {
        this.loanTrancheDisbursementCharge = loanTrancheDisbursementCharge;
    }

    public void updateWaivedAmount(MonetaryCurrency currency) {
        if (isInstalmentFee()) {
            this.amountWaived = BigDecimal.ZERO;
            for (final LoanInstallmentCharge chargePerInstallment : this.loanInstallmentCharge) {
                final Money amountWaived = chargePerInstallment.updateWaivedAndAmountPaidThroughChargePaymentAmount(currency);
                this.amountWaived = this.amountWaived.add(amountWaived.getAmount());
                this.amountOutstanding = this.amountOutstanding.subtract(amountWaived.getAmount());
                if (determineIfFullyPaid() && Money.of(currency, this.amountWaived).isGreaterThanZero()) {
                    this.paid = false;
                    this.waived = true;
                }
            }
            return;
        }

        Money waivedAmount = Money.of(currency, this.amountWaived);
        if (waivedAmount.isGreaterThanZero()) {
            if (waivedAmount.isGreaterThan(this.getAmount(currency))) {
                this.amountWaived = this.getAmount(currency).getAmount();
                this.amountOutstanding = BigDecimal.ZERO;
                this.paid = false;
                this.waived = true;
            } else if (waivedAmount.isLessThan(this.getAmount(currency))) {
                this.paid = false;
                this.waived = false;
            }
        }

    }

    public LoanOverdueInstallmentCharge getOverdueInstallmentCharge() {
        return this.overdueInstallmentCharge;
    }

    public LoanTrancheDisbursementCharge getTrancheDisbursementCharge() {
        return this.loanTrancheDisbursementCharge;
    }

    public Money undoPaidOrPartiallyAmountBy(final Money incrementBy, final Integer installmentNumber, final Money feeAmount) {
        Money processAmount = Money.zero(incrementBy.getCurrency());
        if (isInstalmentFee()) {
            if (installmentNumber == null) {
                processAmount = getLastPaidOrPartiallyPaidInstallmentLoanCharge(incrementBy.getCurrency()).undoPaidAmountBy(incrementBy,
                        feeAmount);
            } else {
                processAmount = getInstallmentLoanCharge(installmentNumber).undoPaidAmountBy(incrementBy, feeAmount);
            }
        } else {
            processAmount = incrementBy;
        }
        Money amountPaidToDate = Money.of(processAmount.getCurrency(), this.amountPaid);

        Money amountDeductedOnThisCharge = Money.zero(processAmount.getCurrency());
        if (processAmount.isGreaterThanOrEqualTo(amountPaidToDate)) {
            amountDeductedOnThisCharge = amountPaidToDate;
            amountPaidToDate = Money.zero(processAmount.getCurrency());
            this.amountPaid = amountPaidToDate.getAmount();
            this.amountOutstanding = this.amount;
            this.paid = false;

        } else {
            amountDeductedOnThisCharge = processAmount;
            amountPaidToDate = amountPaidToDate.minus(processAmount);
            this.amountPaid = amountPaidToDate.getAmount();
            this.amountOutstanding = calculateAmountOutstanding(incrementBy.getCurrency());
        }
        return amountDeductedOnThisCharge;
    }

    public LoanInstallmentCharge getLastPaidOrPartiallyPaidInstallmentLoanCharge(MonetaryCurrency currency) {
        LoanInstallmentCharge paidChargePerInstallment = null;
        for (final LoanInstallmentCharge loanChargePerInstallment : this.loanInstallmentCharge) {
            Money outstanding = Money.of(currency, loanChargePerInstallment.getAmountOutstanding());
            final boolean partiallyPaid = outstanding.isGreaterThanZero()
                    && outstanding.isLessThan(loanChargePerInstallment.getAmount(currency));
            if ((partiallyPaid || loanChargePerInstallment.isPaid())
                    && (paidChargePerInstallment == null || paidChargePerInstallment.getRepaymentInstallment().getDueDate()
                            .isBefore(loanChargePerInstallment.getRepaymentInstallment().getDueDate()))) {
                paidChargePerInstallment = loanChargePerInstallment;
            }
        }
        return paidChargePerInstallment;
    }

    public Loan getLoan() {
        return this.loan;
    }
    
    public boolean isDisbursementCharge() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.DISBURSEMENT);
    }
    
    public boolean isTrancheDisbursementCharge() {
        return ChargeTimeType.fromInt(this.chargeTime).equals(ChargeTimeType.TRANCHE_DISBURSEMENT);
    }
    
    public boolean isDueDateCharge() {
        return this.dueDate != null;
    }
    public static ChargeAdditionalDetails getChargeAdditionalDetailsBean() {
		return chargeAdditionalDetailsBean;
	}

	public static void setChargeAdditionalDetailsBean(ChargeAdditionalDetails chargeAdditionalDetailsBean) {
		LoanCharge.chargeAdditionalDetailsBean = chargeAdditionalDetailsBean;
	}
}