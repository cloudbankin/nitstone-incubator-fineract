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
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.tax.domain.TaxGroupMappings;
import org.joda.time.LocalDate;

/**
 * A wrapper around loan schedule related data exposing needed behaviour by
 * loan.
 */
public class LoanRepaymentScheduleProcessingWrapper {

    public void reprocess(final MonetaryCurrency currency, final LocalDate disbursementDate,
            final List<LoanRepaymentScheduleInstallment> repaymentPeriods, final Set<LoanCharge> loanCharges) {

        Money totalInterest = Money.zero(currency);
        Money totalPrincipal = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentPeriods) {
            totalInterest = totalInterest.plus(installment.getInterestCharged(currency));
            totalPrincipal = totalPrincipal.plus(installment.getPrincipal(currency));
        }
        LocalDate startDate = disbursementDate;
        for (final LoanRepaymentScheduleInstallment period : repaymentPeriods) {

            final Money feeChargesDueForRepaymentPeriod = cumulativeFeeChargesDueWithin(startDate, period.getDueDate(), loanCharges,
                    currency, period, totalPrincipal, totalInterest, !period.isRecalculatedInterestComponent());
            final Money feeChargesWaivedForRepaymentPeriod = cumulativeFeeChargesWaivedWithin(startDate, period.getDueDate(), loanCharges,
                    currency, !period.isRecalculatedInterestComponent());
            final Money feeChargesWrittenOffForRepaymentPeriod = cumulativeFeeChargesWrittenOffWithin(startDate, period.getDueDate(),
                    loanCharges, currency, !period.isRecalculatedInterestComponent());

            final Money penaltyChargesDueForRepaymentPeriod = cumulativePenaltyChargesDueWithin(startDate, period.getDueDate(),
                    loanCharges, currency, period, totalPrincipal, totalInterest, !period.isRecalculatedInterestComponent());
            final Money penaltyChargesWaivedForRepaymentPeriod = cumulativePenaltyChargesWaivedWithin(startDate, period.getDueDate(),
                    loanCharges, currency, !period.isRecalculatedInterestComponent());
            final Money penaltyChargesWrittenOffForRepaymentPeriod = cumulativePenaltyChargesWrittenOffWithin(startDate,
                    period.getDueDate(), loanCharges, currency, !period.isRecalculatedInterestComponent());

            period.updateChargePortion(feeChargesDueForRepaymentPeriod, feeChargesWaivedForRepaymentPeriod,
					feeChargesWrittenOffForRepaymentPeriod, penaltyChargesDueForRepaymentPeriod,
					penaltyChargesWaivedForRepaymentPeriod, penaltyChargesWrittenOffForRepaymentPeriod);

            startDate = period.getDueDate();
        }
    }
    
   
    
    public void reprocessForMonthEnd(final MonetaryCurrency currency, final LocalDate disbursementDate,
            final List<LoanRepaymentScheduleInstallment> repaymentPeriods, final Set<LoanCharge> loanCharges) {

        Money totalInterest = Money.zero(currency);
        Money totalPrincipal = Money.zero(currency);
        for (final LoanRepaymentScheduleInstallment installment : repaymentPeriods) {
            totalInterest = totalInterest.plus(installment.getInterestCharged(currency));
            totalPrincipal = totalPrincipal.plus(installment.getPrincipal(currency));
        }
        LocalDate startDate = disbursementDate;
        for (final LoanRepaymentScheduleInstallment period : repaymentPeriods) {     
         
            period.updateCharges(period.getFeeChargesCharged(currency), period.getPenaltyChargesCharged(currency));

            startDate = period.getDueDate();
        }
    }

    private Money cumulativeFeeChargesDueWithin(final LocalDate periodStart, final LocalDate periodEnd, final Set<LoanCharge> loanCharges,
            final MonetaryCurrency monetaryCurrency, LoanRepaymentScheduleInstallment period, final Money totalPrincipal,
            final Money totalInterest, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(monetaryCurrency);
        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    if (loanCharge.getChargeCalculation().isPercentageBased()) {
                        BigDecimal amount = BigDecimal.ZERO;
                        if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                            amount = amount.add(period.getPrincipal(monetaryCurrency).getAmount()).add(
                                    period.getInterestCharged(monetaryCurrency).getAmount());
                        } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                            amount = amount.add(period.getInterestCharged(monetaryCurrency).getAmount());
                        } else {
                            amount = amount.add(period.getPrincipal(monetaryCurrency).getAmount());
                        }
                        BigDecimal loanChargeAmt = amount.multiply(loanCharge.getPercentage()).divide(BigDecimal.valueOf(100));
                        cumulative = cumulative.plus(loanChargeAmt);
                    } else {
                        cumulative = cumulative.plus(loanCharge.amountOrPercentage());
                    }
                } else if (loanCharge.isOverdueInstallmentCharge()
                        && loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                    cumulative = cumulative.plus(loanCharge.chargeAmount());
                }else if(loanCharge.getChargeCalculation().isPercentageOfOustStanding()) {
                	if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)){
                		if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)){
                			cumulative = cumulative.plus(loanCharge.amount());
                		}
                	}
                } 
                else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                	
                    BigDecimal amount = BigDecimal.ZERO;
                    if(loanCharge.getChargeCalculation().isPercentageOfOustStanding()){
                    	amount = amount.add(loanCharge.amount());
                    }
                    else if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                        amount = amount.add(totalPrincipal.getAmount()).add(totalInterest.getAmount());
                    } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                        amount = amount.add(totalInterest.getAmount());
                    } else {
                        // If charge type is specified due date and loan is
                        // multi disburment loan.
                        // Then we need to get as of this loan charge due date
                        // how much amount disbursed.
                        if (loanCharge.getLoan() != null && loanCharge.isSpecifiedDueDate() && loanCharge.getLoan().isMultiDisburmentLoan()) {
                            for (final LoanDisbursementDetails loanDisbursementDetails : loanCharge.getLoan().getDisbursementDetails()) {
                                if (!loanDisbursementDetails.expectedDisbursementDate().after(loanCharge.getDueDate())) {
                                    amount = amount.add(loanDisbursementDetails.principal());
                                }
                            }
                        } else {
                            amount = amount.add(totalPrincipal.getAmount());
                        }
                    }
                    BigDecimal loanChargeAmt = amount.multiply(loanCharge.getPercentage()).divide(BigDecimal.valueOf(100));
                    /** Habile changes Calculating loan extension service fees */
					LoanChargeAdditionalDetails lcad = loanCharge.getLoanChargeAdditionalDetails();

					if (lcad != null) {
						if (loanCharge.getChargeCalculation().ispercentageOfOutstandingLoanAmount()
								&& loanCharge.isInstallmentRescheduled()
								&& lcad.getIsEnabledFeeCalculationBasedOnTenure() == 1) {
							loanChargeAmt = loanCharge.amount();
							if (lcad.getFeesComponent() != null && lcad.getFeesComponent().doubleValue() > 0
									&& lcad.getIsTaxIncluded() == 0) {
								loanChargeAmt = lcad.getFeesComponent();
							}
							// loanChargeAmt = loanChargeAmt.multiply(new
							// BigDecimal(lcad.getNumberOfDays()));
						} else if (lcad.getIsEnabledFeeCalculationBasedOnTenure() == 1) {
							Integer noOfDays = lcad.getNumberOfDays();
							BigDecimal numberOfDays = new BigDecimal(noOfDays);
							numberOfDays = numberOfDays == null ? BigDecimal.ZERO : numberOfDays;
							loanChargeAmt = loanChargeAmt.multiply(numberOfDays);
						}
					}
					/** Habile changes for charge assigned to only one installment */
					loanCharge.setApplied(true);

					/** Habile changes end */

					/** Habile changes for tax */
					BigDecimal taxComponent = BigDecimal.ZERO;
					BigDecimal feesComponent = BigDecimal.ZERO;
					if (loanCharge.getCharge().getTaxGroup() != null && lcad != null) {
						Iterator<TaxGroupMappings> taxGroupMappings = loanCharge.getCharge().getTaxGroup()
								.getTaxGroupMappings().iterator();
						Set<FinabileLoanChargeTaxDetails> taxComponentDetails = new HashSet<>();

						BigDecimal totalTaxComponentPercentage = BigDecimal.ZERO;
						if (lcad.getIsTaxIncluded() == 1) {
							while (taxGroupMappings.hasNext()) {
								TaxGroupMappings taxGroupMapping = taxGroupMappings.next();
								if (taxGroupMapping.getTaxComponent().getTaxComponentAdditionalDetails()
										.getTaxComponentType().equalsIgnoreCase(lcad.getTaxComponentType())) {
									totalTaxComponentPercentage = totalTaxComponentPercentage
											.add(taxGroupMapping.getTaxComponent().getPercentage());
								}
							}
							feesComponent = loanChargeAmt.divide(
									BigDecimal.ONE.add(totalTaxComponentPercentage.divide(new BigDecimal(100))), 0,
									RoundingMode.HALF_EVEN);
						}

						Iterator<TaxGroupMappings> taxMappings = loanCharge.getCharge().getTaxGroup()
								.getTaxGroupMappings().iterator();
						while (taxMappings.hasNext()) {
							TaxGroupMappings taxGroupMapping = taxMappings.next();
							if (taxGroupMapping.getTaxComponent().getTaxComponentAdditionalDetails()
									.getTaxComponentType().equalsIgnoreCase(lcad.getTaxComponentType())) {

								BigDecimal taxComponentPercentage = taxGroupMapping.getTaxComponent().getPercentage();
								// BigDecimal taxAmount =
								// loanChargeAmt.multiply(taxComponentPercentage).divide(new
								// BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN);

								BigDecimal taxAmount = BigDecimal.ZERO;
								if (lcad.getIsTaxIncluded() == 1) {
									taxAmount = feesComponent.multiply(taxComponentPercentage)
											.divide(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
								} else {
									taxAmount = loanChargeAmt.multiply(taxComponentPercentage)
											.divide(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
								}

								taxComponent = taxComponent.add(taxAmount).setScale(0, RoundingMode.HALF_EVEN);
								FinabileLoanChargeTaxDetails finabileLoanChargeTaxDetails = new FinabileLoanChargeTaxDetails(
										loanCharge, taxGroupMapping.getTaxComponent(),
										taxGroupMapping.getTaxComponent().getName(), taxAmount);
								taxComponentDetails.add(finabileLoanChargeTaxDetails);
							}
						}

						if (lcad.getIsTaxIncluded() == 1) {
							if (lcad.getIsTaxIncluded() == 1) {
								if(!(loanChargeAmt.subtract(taxComponent).doubleValue() == feesComponent.doubleValue())) {
									feesComponent = loanChargeAmt.subtract(taxComponent);
								}
							}
						} else if (lcad.getIsTaxIncluded() == 0) {
							feesComponent = loanChargeAmt;
							loanChargeAmt = loanChargeAmt.add(taxComponent);
						}

						loanCharge.setFinabileLoanChargeTaxDetails(taxComponentDetails);
						lcad.setFeesComponent(Money.of(cumulative.getCurrency(), feesComponent).getAmount());
						lcad.setTaxComponent(Money.of(cumulative.getCurrency(), taxComponent).getAmount());

					}
					/** Habile changes end */
                    cumulative = cumulative.plus(loanChargeAmt);
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.amount());
                }
            }
        }

        return cumulative;
    }

    private Money cumulativeFeeChargesWaivedWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWaived(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWaived(currency));
                }
            }
        }

        return cumulative;
    }

    private Money cumulativeFeeChargesWrittenOffWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWrittenOff(currency));
                }
            }
        }

        return cumulative;
    }

   /* private Money cumulativePenaltyChargesDueWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, LoanRepaymentScheduleInstallment period,
            final Money totalPrincipal, final Money totalInterest, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    if (loanCharge.getChargeCalculation().isPercentageBased()) {
                        BigDecimal amount = BigDecimal.ZERO;
                        if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                            amount = amount.add(period.getPrincipal(currency).getAmount()).add(
                                    period.getInterestCharged(currency).getAmount());
                        } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                            amount = amount.add(period.getInterestCharged(currency).getAmount());
                        } else {
                            amount = amount.add(period.getPrincipal(currency).getAmount());
                        }
                        BigDecimal loanChargeAmt = amount.multiply(loanCharge.getPercentage()).divide(BigDecimal.valueOf(100));
                        cumulative = cumulative.plus(loanChargeAmt);
                    } else {
                        cumulative = cumulative.plus(loanCharge.amountOrPercentage());
                    }
                } else if (loanCharge.isOverdueInstallmentCharge()
                        && loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                    cumulative = cumulative.plus(loanCharge.chargeAmount());
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
                        && loanCharge.getChargeCalculation().isPercentageBased()) {
                    BigDecimal amount = BigDecimal.ZERO;
                    if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
                        amount = amount.add(totalPrincipal.getAmount()).add(totalInterest.getAmount());
                    } else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
                        amount = amount.add(totalInterest.getAmount());
                    } else {
                        amount = amount.add(totalPrincipal.getAmount());
                    }
                    BigDecimal loanChargeAmt = amount.multiply(loanCharge.getPercentage()).divide(BigDecimal.valueOf(100));
                    cumulative = cumulative.plus(loanChargeAmt);
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.amount());
                }
            }
        }

        return cumulative;
    }*/
    
    private Money cumulativePenaltyChargesDueWithin(final LocalDate periodStart, final LocalDate periodEnd,
			final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, LoanRepaymentScheduleInstallment period,
			final Money totalPrincipal, final Money totalInterest, boolean isInstallmentChargeApplicable) {

		Money cumulative = Money.zero(currency);

		for (final LoanCharge loanCharge : loanCharges) {
			if (loanCharge.isPenaltyCharge()) {
				if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
					if (loanCharge.getChargeCalculation().isPercentageBased()) {
						BigDecimal amount = BigDecimal.ZERO;
						if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
							amount = amount.add(period.getPrincipal(currency).getAmount())
									.add(period.getInterestCharged(currency).getAmount());
						} else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
							amount = amount.add(period.getInterestCharged(currency).getAmount());
						} else {
							amount = amount.add(period.getPrincipal(currency).getAmount());
						}
						BigDecimal loanChargeAmt = amount.multiply(loanCharge.getPercentage())
								.divide(BigDecimal.valueOf(100));
						cumulative = cumulative.plus(loanChargeAmt);
					} else {
						cumulative = cumulative.plus(loanCharge.amountOrPercentage());
					}
				} else if (loanCharge.isOverdueInstallmentCharge()
						&& loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
						&& loanCharge.getChargeCalculation().isPercentageBased()
						) {
					cumulative = cumulative.plus(loanCharge.chargeAmount());
					
				} else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
						&& loanCharge.getChargeCalculation().isPercentageBased()
						) {
					BigDecimal amount = BigDecimal.ZERO;
					if (loanCharge.getChargeCalculation().isPercentageOfAmountAndInterest()) {
						amount = amount.add(totalPrincipal.getAmount()).add(totalInterest.getAmount());
					} else if (loanCharge.getChargeCalculation().isPercentageOfInterest()) {
						amount = amount.add(totalInterest.getAmount());
					} else {
						amount = amount.add(totalPrincipal.getAmount());
					}
					BigDecimal loanChargeAmt = amount.multiply(loanCharge.getPercentage())
							.divide(BigDecimal.valueOf(100));
					cumulative = cumulative.plus(loanChargeAmt);
					
				} else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)
						) {
					cumulative = cumulative.plus(loanCharge.amount());
					
				}
			}
		}

		return cumulative;
	}

    private Money cumulativePenaltyChargesWaivedWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWaived(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWaived(currency));
                }
            }
        }

        return cumulative;
    }

    private Money cumulativePenaltyChargesWrittenOffWithin(final LocalDate periodStart, final LocalDate periodEnd,
            final Set<LoanCharge> loanCharges, final MonetaryCurrency currency, boolean isInstallmentChargeApplicable) {

        Money cumulative = Money.zero(currency);

        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                if (loanCharge.isInstalmentFee() && isInstallmentChargeApplicable) {
                    LoanInstallmentCharge loanChargePerInstallment = loanCharge.getInstallmentLoanCharge(periodEnd);
                    if (loanChargePerInstallment != null) {
                        cumulative = cumulative.plus(loanChargePerInstallment.getAmountWrittenOff(currency));
                    }
                } else if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(periodStart, periodEnd)) {
                    cumulative = cumulative.plus(loanCharge.getAmountWrittenOff(currency));
                }
            }
        }

        return cumulative;
    }
}