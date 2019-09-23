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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetails;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetailsRepository;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeAdditionalDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeAdditionalDetailsRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRescheduleRequestToTermVariationMapping;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSummaryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariations;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleDTO;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModelPeriod;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.LoanRescheduleRequestNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoanReschedulePreviewPlatformServiceImpl implements LoanReschedulePreviewPlatformService {

    private final LoanRescheduleRequestRepositoryWrapper loanRescheduleRequestRepository;
    private final LoanUtilService loanUtilService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final DefaultScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
    /*
	 * Habile changes for extension fees Here we initialize some objects
	 */
	private final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository;
	private final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository;
	/*
	 * Habile changes end
	 */

    @Autowired
    public LoanReschedulePreviewPlatformServiceImpl(final LoanRescheduleRequestRepositoryWrapper loanRescheduleRequestRepository,
            final LoanUtilService loanUtilService,
            final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
            final LoanScheduleGeneratorFactory loanScheduleFactory, final LoanSummaryWrapper loanSummaryWrapper,
			final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository,
			final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository) {
        this.loanRescheduleRequestRepository = loanRescheduleRequestRepository;
        this.loanUtilService = loanUtilService;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
        this.loanScheduleFactory = loanScheduleFactory;
        this.loanSummaryWrapper = loanSummaryWrapper;
        this.chargeAdditionalDetailsRepository = chargeAdditionalDetailsRepository;
		this.loanChargeAdditionalDetailsRepository = loanChargeAdditionalDetailsRepository;
    }

    @Override
    public LoanScheduleModel previewLoanReschedule(Long requestId) {
        final LoanRescheduleRequest loanRescheduleRequest = this.loanRescheduleRequestRepository.findOneWithNotFoundDetection(requestId, true);

        if (loanRescheduleRequest == null) { throw new LoanRescheduleRequestNotFoundException(requestId); }

        Loan loan = loanRescheduleRequest.getLoan();

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan,
                loanRescheduleRequest.getRescheduleFromDate());
        LocalDate rescheduleFromDate = null;
        List<LoanTermVariationsData> removeLoanTermVariationsData = new ArrayList<>();
        final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);
        LoanTermVariations dueDateVariationInCurrentRequest = loanRescheduleRequest.getDueDateTermVariationIfExists();
        if(dueDateVariationInCurrentRequest != null){
            for (LoanTermVariationsData loanTermVariation : loanApplicationTerms.getLoanTermVariations().getDueDateVariation()) {
                if (loanTermVariation.getDateValue().equals(dueDateVariationInCurrentRequest.fetchTermApplicaDate())) {
                    rescheduleFromDate = loanTermVariation.getTermApplicableFrom();
                    removeLoanTermVariationsData.add(loanTermVariation);
                }
            }
        }
        loanApplicationTerms.getLoanTermVariations().getDueDateVariation().removeAll(removeLoanTermVariationsData);
        if (rescheduleFromDate == null) {
            rescheduleFromDate = loanRescheduleRequest.getRescheduleFromDate();
        }
        List<LoanTermVariationsData> loanTermVariationsData = new ArrayList<>();
        LocalDate adjustedApplicableDate = null;
        Set<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = loanRescheduleRequest.getLoanRescheduleRequestToTermVariationMappings();
        if (!loanRescheduleRequestToTermVariationMappings.isEmpty()) {
            for (LoanRescheduleRequestToTermVariationMapping loanRescheduleRequestToTermVariationMapping : loanRescheduleRequestToTermVariationMappings) {
                if (loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().getTermType().isDueDateVariation()
                        && rescheduleFromDate != null) {
                    adjustedApplicableDate = loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().fetchDateValue();
                    loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().setTermApplicableFrom(
                            rescheduleFromDate.toDate());
                }
                loanTermVariationsData.add(loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().toData());
            }
        }
        
        for (LoanTermVariationsData loanTermVariation : loanApplicationTerms.getLoanTermVariations().getDueDateVariation()) {
            if (rescheduleFromDate.isBefore(loanTermVariation.getTermApplicableFrom())) {
                LocalDate applicableDate = this.scheduledDateGenerator.generateNextRepaymentDate(rescheduleFromDate, loanApplicationTerms,
                        false);
                if (loanTermVariation.getTermApplicableFrom().equals(applicableDate)) {
                    LocalDate adjustedDate = this.scheduledDateGenerator.generateNextRepaymentDate(adjustedApplicableDate,
                            loanApplicationTerms, false);
                    loanTermVariation.setApplicableFromDate(adjustedDate);
                    loanTermVariationsData.add(loanTermVariation);
                }
            }
        }
        
        loanApplicationTerms.getLoanTermVariations().updateLoanTermVariationsData(loanTermVariationsData);
        final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
        final MathContext mathContext = new MathContext(8, roundingMode);
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.loanRepaymentScheduleTransactionProcessorFactory
                .determineProcessor(loan.transactionProcessingStrategy());
        final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory.create(loanApplicationTerms.getInterestMethod());
        final LoanLifecycleStateMachine loanLifecycleStateMachine = null;
        loan.setHelpers(loanLifecycleStateMachine, this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);
        
        /*
		 * Habile changes for extension fee added into the preview installment
		 */
		BigDecimal extensionFeeAmount = BigDecimal.ZERO;
		int numberOfDays = Days.daysBetween(loanRescheduleRequest.getRescheduleFromDate(),
				loanRescheduleRequest.getDueDateTermVariationIfExists().fetchDateValue()).getDays();
		Charge chargeDefinition = null;
		LoanProduct loanProduct = loan.getLoanProduct();
		for (Charge charge : loanProduct.getLoanProductCharges()) {
			ChargeAdditionalDetails cad = this.chargeAdditionalDetailsRepository.getChargeAdditionalDetails(charge);
			if (charge.isActive() && charge.isInstallmentRescheduled()) {
				chargeDefinition = charge;
				/*
				 * } if (chargeDefinition != null) {
				 */
				LoanCharge newLoanCharge;
				if (cad.getIsEnabledAutoPaid() == 1) {
					newLoanCharge = LoanCharge.createNewFromJson(null, loan, charge, null);
				} else {
					newLoanCharge = LoanCharge.createNewFromJson(
							loanRescheduleRequest.getDueDateTermVariationIfExists().fetchDateValue(), loan, charge,
							null);
				}

				if (cad.getIsEnabledFeeCalculationBasedOnTenure() == 1) {
					/*
					 * BigDecimal value =
					 * newLoanCharge.amountOrPercentage().multiply(loan.getSummary().
					 * getTotalPrincipalOutstanding())
					 * .multiply(BigDecimal.valueOf(numberOfDays)).divide(BigDecimal.valueOf(100l));
					 */
					extensionFeeAmount = extensionFeeAmount.add(newLoanCharge.amountOrPercentage()
							.multiply(loan.getSummary().getTotalPrincipalOutstanding())
							.multiply(BigDecimal.valueOf(numberOfDays)).divide(BigDecimal.valueOf(100l)));
				} else {
					/*
					 * BigDecimal value =
					 * newLoanCharge.amountOrPercentage().multiply(loan.getSummary().
					 * getTotalPrincipalOutstanding()) .divide(BigDecimal.valueOf(100l));
					 */
					extensionFeeAmount = extensionFeeAmount.add(newLoanCharge.amountOrPercentage()
							.multiply(loan.getSummary().getTotalPrincipalOutstanding())
							.divide(BigDecimal.valueOf(100l)));
				}

			}
			extensionFeeAmount = extensionFeeAmount.setScale(0, RoundingMode.HALF_EVEN);
		}

		//
        final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallments(mathContext, loanApplicationTerms,
                loan, loanApplicationTerms.getHolidayDetailDTO(),
                loanRepaymentScheduleTransactionProcessor, rescheduleFromDate);
        final LoanScheduleModel loanScheduleModel = loanSchedule.getLoanScheduleModel();
        for (LoanCharge charge : loan.getLoanCharges()) {
			LoanChargeAdditionalDetails lcad = this.loanChargeAdditionalDetailsRepository
					.getLoanChargeAdditionalDetails(charge);
			if (charge.isActive() && charge.isInstallmentRescheduled() && lcad.getIsEnabledAutoPaid() == 1) {
				extensionFeeAmount = extensionFeeAmount.add(charge.getAmount(loan.getCurrency()).getAmount());
				/*
				 * if(lcad != null && lcad.getIsEnabledAutoPaid() == 0) { extensionFeeAmount =
				 * extensionFeeAmount.subtract(charge.getAmountPaid(loan.getCurrency()).
				 * getAmount()); }
				 */
			}

		}
		int i = 1;
		for (LoanScheduleModelPeriod schedule : loanScheduleModel.getPeriods()) {
			if (i != 1 && schedule.periodDueDate()
					.equals(loanRescheduleRequest.getDueDateTermVariationIfExists().fetchDateValue())) {
				if (schedule.feeChargesDue() != null || schedule.feeChargesDue() != BigDecimal.ZERO) {
					schedule.addLoanCharges(extensionFeeAmount, BigDecimal.ZERO);
				} else {
					schedule.addLoanCharges(extensionFeeAmount, BigDecimal.ZERO);
				}
			}
			i++;
		}
		/*
		 * Habile changes end
		 */
        LoanScheduleModel loanScheduleModels = LoanScheduleModel.withLoanScheduleModelPeriods(loanScheduleModel.getPeriods(),
                loanScheduleModel);
        /*
		 * Habile changes for extension fees
		 */
		loanScheduleModels
				.setTotalFeeChargesCharged(loanScheduleModels.getTotalFeeChargesCharged().add(extensionFeeAmount));
		loanScheduleModels.setTotalOutstanding(loanScheduleModels.getTotalOutstanding().add(extensionFeeAmount));
		/*
		 * 
		 */
        
        return loanScheduleModels;
    }

}
