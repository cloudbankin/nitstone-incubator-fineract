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
package org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargePaidDetail;
import org.apache.fineract.portfolio.loanaccount.domain.ChangedTransactionDetail;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidBy;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleProcessingWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionToRepaymentScheduleMapping;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl.CreocoreLoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl.HeavensFamilyLoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl.InterestPrincipalPenaltyFeesOrderLoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.joda.time.LocalDate;
/**
 * Abstract implementation of {@link LoanRepaymentScheduleTransactionProcessor}
 * which is more convenient for concrete implementations to extend.
 * 
 * @see InterestPrincipalPenaltyFeesOrderLoanRepaymentScheduleTransactionProcessor
 * 
 * @see HeavensFamilyLoanRepaymentScheduleTransactionProcessor
 * @see CreocoreLoanRepaymentScheduleTransactionProcessor
 */
public abstract class AbstractLoanRepaymentScheduleTransactionProcessor implements LoanRepaymentScheduleTransactionProcessor {

    /**
     * Provides support for passing all {@link LoanTransaction}'s so it will
     * completely re-process the entire loan schedule. This is required in cases
     * where the {@link LoanTransaction} being processed is in the past and
     * falls before existing transactions or and adjustment is made to an
     * existing in which case the entire loan schedule needs to be re-processed.
     */

    @Override
    public ChangedTransactionDetail handleTransaction(final LocalDate disbursementDate,
            final List<LoanTransaction> transactionsPostDisbursement, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {

        if (charges != null) {
            for (final LoanCharge loanCharge : charges) {
                if (!loanCharge.isDueAtDisbursement()) {
                    loanCharge.resetPaidAmount(currency);
                }
            }
        }

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            currentInstallment.resetDerivedComponents();
            currentInstallment.updateDerivedFields(currency, disbursementDate);
        }

        // re-process loan charges over repayment periods (picking up on waived
        // loan charges)
        final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
        
        
        wrapper.reprocessForMonthEnd(currency, disbursementDate, installments, charges);
        
        //wrapper.reprocess(currency, disbursementDate, installments, charges);
        
    
        
        final ChangedTransactionDetail changedTransactionDetail = new ChangedTransactionDetail();
        final List<LoanTransaction> transactionstoBeProcessed = new ArrayList<>();
        for (final LoanTransaction loanTransaction : transactionsPostDisbursement) {
            if (loanTransaction.isChargePayment()) {
                List<LoanChargePaidDetail> chargePaidDetails = new ArrayList<>();
                final Set<LoanChargePaidBy> chargePaidBies = loanTransaction.getLoanChargesPaid();
                final Set<LoanCharge> transferCharges = new HashSet<>();
                for (final LoanChargePaidBy chargePaidBy : chargePaidBies) {
                    LoanCharge loanCharge = chargePaidBy.getLoanCharge();
                    transferCharges.add(loanCharge);
                    if (loanCharge.isInstalmentFee()) {
                        chargePaidDetails.addAll(loanCharge.fetchRepaymentInstallment(currency));
                    }
                }
                LocalDate startDate = disbursementDate;
                for (final LoanRepaymentScheduleInstallment installment : installments) {
                    for (final LoanCharge loanCharge : transferCharges) {
                        if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(startDate, installment.getDueDate())) {
                            Money amountForProcess = loanCharge.getAmount(currency);
                            if (amountForProcess.isGreaterThan(loanTransaction.getAmount(currency))) {
                                amountForProcess = loanTransaction.getAmount(currency);
                            }
                            LoanChargePaidDetail chargePaidDetail = new LoanChargePaidDetail(amountForProcess, installment,
                                    loanCharge.isFeeCharge());
                            chargePaidDetails.add(chargePaidDetail);
                            break;
                        }
                    }
                    startDate = installment.getDueDate();
                }
                loanTransaction.resetDerivedComponents();
                Money unprocessed = loanTransaction.getAmount(currency);
                for (LoanChargePaidDetail chargePaidDetail : chargePaidDetails) {
                    final List<LoanRepaymentScheduleInstallment> processInstallments = new ArrayList<>(1);
                    processInstallments.add(chargePaidDetail.getInstallment());
                    Money processAmt = chargePaidDetail.getAmount();
                    if (processAmt.isGreaterThan(unprocessed)) {
                        processAmt = unprocessed;
                    }
                    unprocessed = handleTransactionAndCharges(loanTransaction, currency, processInstallments, transferCharges, processAmt,
                            chargePaidDetail.isFeeCharge(), loanTransaction.getLoan().getLoanProduct());
                    if (!unprocessed.isGreaterThanZero()) {
                        break;
                    }
                }

                if (unprocessed.isGreaterThanZero()) {
                    onLoanOverpayment(loanTransaction, unprocessed);
                    loanTransaction.updateOverPayments(unprocessed);
                }

            } else {
                transactionstoBeProcessed.add(loanTransaction);
            }
        }
        /* HABILE CHANGES*/
        final List<LoanTransaction> incomePostingTransactionsTemp = new ArrayList<>();
        for (final LoanTransaction loanTransaction : transactionstoBeProcessed) {
        	if(loanTransaction.isIncomePosting())
        	{
        		incomePostingTransactionsTemp.add(loanTransaction);
        	}
        }
      
        List<LoanRepaymentScheduleInstallment> installmentsTemp=installments;
        //List<LoanRepaymentScheduleInstallment> installmentsTempTemp=installments;
        LocalDate previousTransactionDate=null;
        /* HABILE CHANGES*/
        for (final LoanTransaction loanTransaction : transactionstoBeProcessed) {

        	if (!loanTransaction.getTypeOf().equals(LoanTransactionType.REFUND_FOR_ACTIVE_LOAN)) {
                final Comparator<LoanRepaymentScheduleInstallment> byDate = new Comparator<LoanRepaymentScheduleInstallment>() {

                    @Override
                    public int compare(LoanRepaymentScheduleInstallment ord1, LoanRepaymentScheduleInstallment ord2) {
                        return ord1.getDueDate().compareTo(ord2.getDueDate());
                    }
                };
                Collections.sort(installments, byDate);
            }
        	/* HABILE CHANGES*/
        	installmentsTemp=installments;
        	/* HABILE CHANGES*/
        	
            if (loanTransaction.isRepayment() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment()) {
               /*HABILE CHANGES*/
            	int i=0;
            	Money postingAmount=Money.zero(currency);Money feePostingAmount=Money.zero(currency);Money penaltyPostingAmount=Money.zero(currency);
            		 for (LoanTransaction incomePostingTransactionsIndividual : incomePostingTransactionsTemp) {
            		 if((incomePostingTransactionsIndividual.getTransactionDate().isBefore(loanTransaction.getTransactionDate())
            				 && (previousTransactionDate==null|| 
            				 (previousTransactionDate!=null && 
            				 (incomePostingTransactionsIndividual.getTransactionDate().isAfter(previousTransactionDate)
            						 ||incomePostingTransactionsIndividual.getTransactionDate().equals(previousTransactionDate))))) ||
            				 (LoanWritePlatformServiceJpaRepositoryImpl.prepay==1 && incomePostingTransactionsIndividual.getTransactionDate().equals(loanTransaction.getTransactionDate())))
            		 {
            			
            			 
            			 postingAmount= incomePostingTransactionsIndividual.getInterestPortion(currency);
            			 feePostingAmount = incomePostingTransactionsIndividual.getFeeChargesPortion(currency);
            			 penaltyPostingAmount = incomePostingTransactionsIndividual.getPenaltyChargesPortion(currency);
            			 int j=0;
            			 for (j=0;j<installmentsTemp.size();j++) {
            				 
            				 if(postingAmount.isGreaterThanZero()) {
            				 LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment=installmentsTemp.get(j);
            				
            				 	BigDecimal interestIncome=loanRepaymentScheduleInstallment.getPendingInterestIncome(currency).getAmount();            				 
            					 if(Money.of(currency, interestIncome).isGreaterThan(postingAmount))
            					 {
            						 loanRepaymentScheduleInstallment.setInterestIncome(postingAmount.getAmount(),currency);
            						 postingAmount=Money.zero(currency);
            						 
            					 }
            					 else
            					 {
            						 interestIncome=loanRepaymentScheduleInstallment.getPendingInterestIncome(currency).getAmount();
            						 loanRepaymentScheduleInstallment.setInterestIncome(interestIncome,currency);
            						 postingAmount=postingAmount.minus(interestIncome);
            						 
            					 }
            					 
            				
            				 installmentsTemp.set(j,loanRepaymentScheduleInstallment);
            				 
            			
            			 }
            				 if(penaltyPostingAmount.isGreaterThanZero()){
                				 LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment=installmentsTemp.get(j);
                				
                				 
                				 BigDecimal penaltyIncome = loanRepaymentScheduleInstallment.getPendingPenaltyIncome(currency).getAmount();
                				 
                					
                					 if(Money.of(currency, penaltyIncome).isGreaterThan(penaltyPostingAmount))
                					 {
                						 loanRepaymentScheduleInstallment.setPenaltyIncome(penaltyPostingAmount.getAmount(),currency);
                						 penaltyPostingAmount=Money.zero(currency);
                						 
                					 }
                					 else
                					 {
                						 penaltyIncome=loanRepaymentScheduleInstallment.getPendingPenaltyIncome(currency).getAmount();                						
                						 penaltyPostingAmount=penaltyPostingAmount.minus(penaltyIncome);
                						 loanRepaymentScheduleInstallment.setPenaltyIncome(penaltyIncome,currency);
                						 
                					 }
                				
                				 installmentsTemp.set(j,loanRepaymentScheduleInstallment);
                				 
                			
                			 }
            				 if(feePostingAmount.isGreaterThanZero()){
                				 LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment=installmentsTemp.get(j);
                				
                				 
                				 BigDecimal feeIncome = loanRepaymentScheduleInstallment.getPendingFeeIncome(currency).getAmount();
                					
                					 if(Money.of(currency, feeIncome).isGreaterThan(feePostingAmount))
                					 {
                						 loanRepaymentScheduleInstallment.setFeeIncome(feePostingAmount.getAmount(),currency);
                						 feePostingAmount=Money.zero(currency);
                						 
                					 }
                					 else
                					 {
                						 feeIncome=loanRepaymentScheduleInstallment.getPendingFeeIncome(currency).getAmount();                						
                						 feePostingAmount=feePostingAmount.minus(feeIncome);
                						 loanRepaymentScheduleInstallment.setFeeIncome(feeIncome,currency);
                						 
                					 }
                				
                				 installmentsTemp.set(j,loanRepaymentScheduleInstallment);
                				 
                			
                			 }
            			 }
            			 
            		 }
            		 if(LoanWritePlatformServiceJpaRepositoryImpl.prepay==1) {
            			 if( postingAmount.isGreaterThanZero()  ||  feePostingAmount.isGreaterThanZero() ||  penaltyPostingAmount.isGreaterThanZero()) {
            				 LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment=installmentsTemp.get(installmentsTemp.size() -1);
            				 if(postingAmount.isGreaterThanZero()) {
            					 loanRepaymentScheduleInstallment.setInterestCharged(postingAmount.getAmount(), currency);
            					 loanRepaymentScheduleInstallment.setInterestIncome(postingAmount.getAmount(), currency);
            					
            				 }
            				 else if(feePostingAmount.isGreaterThanZero()) {  
            				 loanRepaymentScheduleInstallment.setFeeCharged(feePostingAmount.getAmount(), currency);
            				 loanRepaymentScheduleInstallment.setFeeIncome(feePostingAmount.getAmount(), currency);
            				 }
            				 else if(penaltyPostingAmount.isGreaterThanZero()) {
            					 loanRepaymentScheduleInstallment.setPenaltyCharged(penaltyPostingAmount.getAmount(), currency);
            					 loanRepaymentScheduleInstallment.setPenaltyIncome(penaltyPostingAmount.getAmount(), currency);
            				 }
            				 installmentsTemp.set(installmentsTemp.size() -1, loanRepaymentScheduleInstallment);
            			 }
            		 }
            		
            		 i=i+1;
            		 }
            		 previousTransactionDate= loanTransaction.getTransactionDate();
            	/* HABILE CHANGES*/
            	// pass through for new transactions
                if (loanTransaction.getId() == null) {
                	/* HABILE CHANGES*/
                    handleTransaction(loanTransaction, currency, installmentsTemp, charges);
                    /* HABILE CHANGES*/
                    loanTransaction.adjustInterestComponent(currency);
                } else {
                    /**
                     * For existing transactions, check if the re-payment
                     * breakup (principal, interest, fees, penalties) has
                     * changed.<br>
                     **/
                    final LoanTransaction newLoanTransaction = LoanTransaction.copyTransactionProperties(loanTransaction);

                    // Reset derived component of new loan transaction and
                    // re-process transaction
                    /* HABILE CHANGES*/
                    handleTransaction(newLoanTransaction, currency, installmentsTemp, charges);
                    /* HABILE CHANGES*/
                    newLoanTransaction.adjustInterestComponent(currency);
                    /**
                     * Check if the transaction amounts have changed. If so,
                     * reverse the original transaction and update
                     * changedTransactionDetail accordingly
                     **/
                    Money newLoanTransactionTemp=loanTransaction.getInterestPortion(currency);
                    BigDecimal newLoanTransactionTempBigDecimal=newLoanTransactionTemp.getAmountDefaultedToNullIfZero();
                    if(newLoanTransactionTempBigDecimal==null || (incomePostingTransactionsTemp!=null && incomePostingTransactionsTemp.size()>0)){
                    if (LoanTransaction.transactionAmountsMatch(currency, loanTransaction, newLoanTransaction)) {
                        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(newLoanTransaction
                                .getLoanTransactionToRepaymentScheduleMappings());
                    } else {
                        loanTransaction.reverse();
                        loanTransaction.updateExternalId(null);
                        changedTransactionDetail.getNewTransactionMappings().put(loanTransaction.getId(), newLoanTransaction);
                    }
                    }
                }

            } else if (loanTransaction.isWriteOff()) {
                loanTransaction.resetDerivedComponents();
                handleWriteOffLoan(loanTransaction, currency, installments);
            } else if (loanTransaction.isRefundForActiveLoan()) {
                loanTransaction.resetDerivedComponents();

                handleRefund(loanTransaction, currency, installments, charges);
            }
        }
        return changedTransactionDetail;
    }

    /**
     * Provides support for processing the latest transaction (which should be
     * latest transaction) against the loan schedule.
     */
    @Override
    public void handleTransaction(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {

        final Money amountToProcess = null;
        final boolean isChargeAmount = false;
        handleTransaction(loanTransaction, currency, installments, charges, amountToProcess, isChargeAmount);

    }

    private void handleTransaction(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges, final Money chargeAmountToProcess,
            final boolean isFeeCharge) {

        Money transactionAmountUnprocessed = handleTransactionAndCharges(loanTransaction, currency, installments, charges,
                chargeAmountToProcess, isFeeCharge, loanTransaction.getLoan().getLoanProduct());

        if (transactionAmountUnprocessed.isGreaterThanZero()) {
            if (loanTransaction.isWaiver()) {
                loanTransaction.updateComponentsAndTotal(transactionAmountUnprocessed.zero(), transactionAmountUnprocessed.zero(),
                        transactionAmountUnprocessed.zero(), transactionAmountUnprocessed.zero());
            } else {
                onLoanOverpayment(loanTransaction, transactionAmountUnprocessed);
                loanTransaction.updateOverPayments(transactionAmountUnprocessed);
            }
        }
    }

    private Money handleTransactionAndCharges(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges, final Money chargeAmountToProcess,
            final boolean isFeeCharge, final LoanProduct loanProduct) {
        // to.
        if (loanTransaction.isRepayment() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment()) {
            loanTransaction.resetDerivedComponents();
        }
        
        
        Money transactionAmountUnprocessed = Money.zero(currency);
        
        if(!loanProduct.isNewProductConfig())
        	transactionAmountUnprocessed =	processTransactionAtEnd(loanTransaction, currency, installments, chargeAmountToProcess);
        
        else if(loanProduct.isNewProductConfig())
            transactionAmountUnprocessed=	processTransactionNewConfig(loanTransaction, currency, installments, chargeAmountToProcess);

        final Set<LoanCharge> loanFees = extractFeeCharges(charges);
        final Set<LoanCharge> loanPenalties = extractPenaltyCharges(charges);
        Integer installmentNumber = null;
        if (loanTransaction.isChargePayment() && installments.size() == 1) {
            installmentNumber = installments.get(0).getInstallmentNumber();
        }

        if (loanTransaction.isNotWaiver()) {
            Money feeCharges = loanTransaction.getFeeChargesPortion(currency);
            Money penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
            if (chargeAmountToProcess != null && feeCharges.isGreaterThan(chargeAmountToProcess)) {
                if (isFeeCharge) {
                    feeCharges = chargeAmountToProcess;
                } else {
                    penaltyCharges = chargeAmountToProcess;
                }
            }
            if (feeCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, feeCharges, loanFees, installmentNumber);
            }

            if (penaltyCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, penaltyCharges, loanPenalties, installmentNumber);
            }
        }
        return transactionAmountUnprocessed;
    }

    private Money processTransaction(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, Money amountToProcess) {
        int installmentIndex = 0;

        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        Loan loan = loanTransaction.getLoan();
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);
        if (amountToProcess != null) {
            transactionAmountUnprocessed = amountToProcess;
        }
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();
      
        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            if (transactionAmountUnprocessed.isGreaterThanZero()) {
                if (currentInstallment.isNotFullyPaidOff()) {

                    // is this transaction early/late/on-time with respect to
                    // the
                    // current installment?         
                	currentInstallment.setInterestIncome(null, currency);
                	LocalDate compondingDate = currentInstallment.getFromDate().plusMonths(1).withDayOfMonth(1).minusDays(1);
                	Money interestPost = Money.zero(currency); Money feePost = Money.zero(currency); Money penaltyPost = Money.zero(currency);
                  	if(!transactionDate.isBefore(compondingDate)) {
	                	if(loan != null) {
	                		interestPost =  loan.getReceivableInterestIncomePostingForInstallment(compondingDate);
	                		feePost = loan.getReceivableFeeIncomePostingForInstallment(compondingDate);
	                		penaltyPost = loan.getReceivablePenaltyIncomePostingForInstallment(compondingDate);	                		               	
	                	}
                	}
                	interestPost = interestPost;
                	currentInstallment.setInterestIncome(interestPost.getAmount(), currency);
                	currentInstallment.setFeeIncome(feePost.getAmount(), currency);
                	currentInstallment.setPenaltyIncome(penaltyPost.getAmount(), currency);
                	
                    if (isTransactionInAdvanceOfInstallment(installmentIndex, installments, transactionDate, transactionAmountUnprocessed)) {
                        transactionAmountUnprocessed = handleTransactionThatIsPaymentInAdvanceOfInstallment(currentInstallment,
                                installments, loanTransaction, transactionDate, transactionAmountUnprocessed, transactionMappings);
                    } else if (isTransactionALateRepaymentOnInstallment(installmentIndex, installments,
                            loanTransaction.getTransactionDate())) {
                        // does this result in a late payment of existing
                        // installment?
                       /* transactionAmountUnprocessed = handleTransactionThatIsALateRepaymentOfInstallment(currentInstallment, installments,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);*/
                    	transactionAmountUnprocessed = handleTransactionThatIsOnTimePaymentOfInstallment(currentInstallment,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    } else {
                        // standard transaction
                        transactionAmountUnprocessed = handleTransactionThatIsOnTimePaymentOfInstallment(currentInstallment,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    }
                }               
            }

            installmentIndex++;
        }
        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
        return transactionAmountUnprocessed;
    }
    
    private Money processTransactionNewConfig(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, Money amountToProcess) {
        int installmentIndex = 0;

        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);
        if (amountToProcess != null) {
            transactionAmountUnprocessed = amountToProcess;
        }
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            if (transactionAmountUnprocessed.isGreaterThanZero()) {
                if (currentInstallment.isNotFullyPaidOff()) {

                    // is this transaction early/late/on-time with respect to
                    // the
                    // current installment?
                    if (isTransactionInAdvanceOfInstallment(installmentIndex, installments, transactionDate, transactionAmountUnprocessed)) {
                        transactionAmountUnprocessed = handleTransactionThatIsPaymentInAdvanceOfInstallment(currentInstallment,
                                installments, loanTransaction, transactionDate, transactionAmountUnprocessed, transactionMappings);
                    } else if (isTransactionALateRepaymentOnInstallment(installmentIndex, installments,
                            loanTransaction.getTransactionDate())) {
                        // does this result in a late payment of existing
                        // installment?
                        transactionAmountUnprocessed = handleTransactionThatIsALateRepaymentOfInstallment(currentInstallment, installments,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    } else {
                        // standard transaction
                        transactionAmountUnprocessed = handleTransactionThatIsOnTimePaymentOfInstallment(currentInstallment,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    }
                }
            }

            installmentIndex++;
        }
        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
        return transactionAmountUnprocessed;
    }
    
    /** habile Changes **/
    private Money processTransactionAtEnd(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, Money amountToProcess) {
        int installmentIndex = 0;

        final LocalDate transactionDate = loanTransaction.getTransactionDate();      
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);
        if (amountToProcess != null) {
            transactionAmountUnprocessed = amountToProcess;
        }
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            if (transactionAmountUnprocessed.isGreaterThanZero()) {
                if (currentInstallment.isNotFullyPaidOff()) {

                    // is this transaction early/late/on-time with respect to
                    // the
                    // current installment?                  	
                	
                    if (isTransactionInAdvanceOfInstallment(installmentIndex, installments, transactionDate, transactionAmountUnprocessed)) {
                        transactionAmountUnprocessed = handleTransactionThatIsPaymentInAdvanceOfInstallment(currentInstallment,
                                installments, loanTransaction, transactionDate, transactionAmountUnprocessed, transactionMappings);
                    } else if (isTransactionALateRepaymentOnInstallment(installmentIndex, installments,
                            loanTransaction.getTransactionDate())) {
                        // does this result in a late payment of existing
                        // installment?
                       /* transactionAmountUnprocessed = handleTransactionThatIsALateRepaymentOfInstallment(currentInstallment, installments,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);*/
                    	transactionAmountUnprocessed = handleTransactionThatIsOnTimePaymentOfInstallment(currentInstallment,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    } else {
                        // standard transaction
                        transactionAmountUnprocessed = handleTransactionThatIsOnTimePaymentOfInstallment(currentInstallment,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    }
                }
            }

            installmentIndex++;
        }
        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
        return transactionAmountUnprocessed;
    }

    private Set<LoanCharge> extractFeeCharges(final Set<LoanCharge> loanCharges) {
        final Set<LoanCharge> feeCharges = new HashSet<>();
        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge()) {
                feeCharges.add(loanCharge);
            }
        }
        return feeCharges;
    }

    private Set<LoanCharge> extractPenaltyCharges(final Set<LoanCharge> loanCharges) {
        final Set<LoanCharge> penaltyCharges = new HashSet<>();
        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                penaltyCharges.add(loanCharge);
            }
        }
        return penaltyCharges;
    }

    private void updateChargesPaidAmountBy(final LoanTransaction loanTransaction, final Money feeCharges, final Set<LoanCharge> charges,
            final Integer installmentNumber) {

        Money amountRemaining = feeCharges;
        while (amountRemaining.isGreaterThanZero()) {
            final LoanCharge unpaidCharge = findEarliestUnpaidChargeFromUnOrderedSet(charges, feeCharges.getCurrency());
            Money feeAmount = feeCharges.zero();
            if (loanTransaction.isChargePayment()) {
                feeAmount = feeCharges;
            }
            if (unpaidCharge == null) break; // All are trache charges
            final Money amountPaidTowardsCharge = unpaidCharge.updatePaidAmountBy(amountRemaining, installmentNumber, feeAmount);
            if (!amountPaidTowardsCharge.isZero()) {
                Set<LoanChargePaidBy> chargesPaidBies = loanTransaction.getLoanChargesPaid();
                if (loanTransaction.isChargePayment()) {
                    for (final LoanChargePaidBy chargePaidBy : chargesPaidBies) {
                        LoanCharge loanCharge = chargePaidBy.getLoanCharge();
                        if (loanCharge.getId().equals(unpaidCharge.getId())) {
                            chargePaidBy.setAmount(amountPaidTowardsCharge.getAmount());
                        }
                    }
                } else {
                    final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(loanTransaction, unpaidCharge,
                            amountPaidTowardsCharge.getAmount(), installmentNumber);
                    chargesPaidBies.add(loanChargePaidBy);
                }
                amountRemaining = amountRemaining.minus(amountPaidTowardsCharge);
            }
        }

    }

    private LoanCharge findEarliestUnpaidChargeFromUnOrderedSet(final Set<LoanCharge> charges, final MonetaryCurrency currency) {
        LoanCharge earliestUnpaidCharge = null;
        LoanCharge installemntCharge = null;
        LoanInstallmentCharge chargePerInstallment = null;
        for (final LoanCharge loanCharge : charges) {
            if (loanCharge.getAmountOutstanding(currency).isGreaterThanZero() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee()) {
                    LoanInstallmentCharge unpaidLoanChargePerInstallment = loanCharge.getUnpaidInstallmentLoanCharge();
                    if (chargePerInstallment == null
                            || chargePerInstallment.getRepaymentInstallment().getDueDate()
                                    .isAfter(unpaidLoanChargePerInstallment.getRepaymentInstallment().getDueDate())) {
                        installemntCharge = loanCharge;
                        chargePerInstallment = unpaidLoanChargePerInstallment;
                    }
                } else if (earliestUnpaidCharge == null || loanCharge.getDueLocalDate().isBefore(earliestUnpaidCharge.getDueLocalDate())) {
                    earliestUnpaidCharge = loanCharge;
                }
            }
        }
        if (earliestUnpaidCharge == null
                || (chargePerInstallment != null && earliestUnpaidCharge.getDueLocalDate().isAfter(
                        chargePerInstallment.getRepaymentInstallment().getDueDate()))) {
            earliestUnpaidCharge = installemntCharge;
        }

        return earliestUnpaidCharge;
    }

    @Override
    public void handleWriteOff(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments) {

        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        Money principalPortion = Money.zero(currency);
        Money interestPortion = Money.zero(currency);
        Money feeChargesPortion = Money.zero(currency);
        Money penaltychargesPortion = Money.zero(currency);

        // determine how much is written off in total and breakdown for
        // principal, interest and charges
        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {

            if (currentInstallment.isNotFullyPaidOff()) {
                principalPortion = principalPortion.plus(currentInstallment.writeOffOutstandingPrincipal(transactionDate, currency));
                interestPortion = interestPortion.plus(currentInstallment.writeOffOutstandingInterest(transactionDate, currency));
                feeChargesPortion = feeChargesPortion.plus(currentInstallment.writeOffOutstandingFeeCharges(transactionDate, currency));
                penaltychargesPortion = penaltychargesPortion.plus(currentInstallment.writeOffOutstandingPenaltyCharges(transactionDate,
                        currency));
            }
        }

        loanTransaction.updateComponentsAndTotal(principalPortion, interestPortion, feeChargesPortion, penaltychargesPortion);
    }
    
    @Override
    public void handleWriteOffLoan(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments) {

        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        Money principalPortion = Money.zero(currency);
        Money interestPortion = Money.zero(currency);
        Money feeChargesPortion = Money.zero(currency);
        Money penaltychargesPortion = Money.zero(currency);
            
        // determine how much is written off in total and breakdown for
        // principal, interest and charges
        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
        	
            if (currentInstallment.isNotFullyPaidOff()) {
            	principalPortion = principalPortion.plus(currentInstallment.writeOffOutstandingPrincipal(transactionDate, currency));
                interestPortion = interestPortion.plus(currentInstallment.writeOffOutstandingPostingInterest(transactionDate, currency));
                feeChargesPortion = feeChargesPortion.plus(currentInstallment.writeOffOutstandingFeeCharges(transactionDate, currency));
                penaltychargesPortion = penaltychargesPortion.plus(currentInstallment.writeOffOutstandingPenaltyCharges(transactionDate,
                        currency));
            }
          
        }
        Loan loan = loanTransaction.getLoan();
        Money totalInterestPosting = loan.getReceivableInterestIncomePosting(transactionDate);
        totalInterestPosting = totalInterestPosting.minus(interestPortion.getAmount());
        interestPortion = interestPortion.plus(totalInterestPosting);
        LoanRepaymentScheduleInstallment lastInstallment = installments.get(installments.size()-1);
        lastInstallment.setInterestwriteOff(totalInterestPosting, currency);
        loanTransaction.updateComponentsAndTotal(principalPortion, interestPortion, feeChargesPortion, penaltychargesPortion);
    }

    // abstract interface
    /**
     * This method is responsible for checking if the current transaction is 'an
     * advance/early payment' based on the details passed through.
     * 
     * Default implementation simply processes transactions as 'Late' if the
     * transaction date is after the installment due date.
     */
    protected boolean isTransactionALateRepaymentOnInstallment(final int installmentIndex,
            final List<LoanRepaymentScheduleInstallment> installments, final LocalDate transactionDate) {

        final LoanRepaymentScheduleInstallment currentInstallment = installments.get(installmentIndex);

        return transactionDate.isAfter(currentInstallment.getDueDate());
    }

    /**
     * For late repayments, how should components of installment be paid off
     * 
     * @param transactionMappings
     *            TODO
     */
    protected abstract Money handleTransactionThatIsALateRepaymentOfInstallment(final LoanRepaymentScheduleInstallment currentInstallment,
            final List<LoanRepaymentScheduleInstallment> installments, final LoanTransaction loanTransaction,
            final Money transactionAmountUnprocessed, final List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    /**
     * This method is responsible for checking if the current transaction is 'an
     * advance/early payment' based on the details passed through.
     * 
     * Default implementation is check transaction date is before installment
     * due date.
     */
    protected boolean isTransactionInAdvanceOfInstallment(final int currentInstallmentIndex,
            final List<LoanRepaymentScheduleInstallment> installments, final LocalDate transactionDate,
            @SuppressWarnings("unused") final Money transactionAmount) {

        final LoanRepaymentScheduleInstallment currentInstallment = installments.get(currentInstallmentIndex);

        return transactionDate.isBefore(currentInstallment.getDueDate());
    }

    /**
     * For early/'in advance' repayments.
     * 
     * @param transactionMappings
     *            TODO
     */
    protected abstract Money handleTransactionThatIsPaymentInAdvanceOfInstallment(
            final LoanRepaymentScheduleInstallment currentInstallment, final List<LoanRepaymentScheduleInstallment> installments,
            final LoanTransaction loanTransaction, final LocalDate transactionDate, final Money paymentInAdvance,
            final List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    /**
     * For normal on-time repayments.
     * 
     * @param transactionMappings
     *            TODO
     */
    protected abstract Money handleTransactionThatIsOnTimePaymentOfInstallment(final LoanRepaymentScheduleInstallment currentInstallment,
            final LoanTransaction loanTransaction, final Money transactionAmountUnprocessed,
            final List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    /**
     * Invoked when a transaction results in an over-payment of the full loan.
     * 
     * transaction amount is greater than the total expected principal and
     * interest of the loan.
     */
    @SuppressWarnings("unused")
    protected void onLoanOverpayment(final LoanTransaction loanTransaction, final Money loanOverPaymentAmount) {
        // empty implementation by default.
    }

    @Override
    public Money handleRepaymentSchedule(final List<LoanTransaction> transactionsPostDisbursement, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments) {
        Money unProcessed = Money.zero(currency);
        for (final LoanTransaction loanTransaction : transactionsPostDisbursement) {
            Money amountToProcess = null;
            if (loanTransaction.isRepayment() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment()) {
                loanTransaction.resetDerivedComponents();
            }
            if (loanTransaction.isInterestWaiver()) {
            	// Habile Arivu new config
            	if(!loanTransaction.getLoan().getLoanProduct().isNewProductConfig())
            		processTransaction(loanTransaction, currency, installments, amountToProcess);
            	
            	else if(loanTransaction.getLoan().getLoanProduct().isNewProductConfig())
            		processTransactionNewConfig(loanTransaction, currency, installments, amountToProcess);
            	
            } else {
            	// Habile Arivu new config
            	if(!loanTransaction.getLoan().getLoanProduct().isNewProductConfig())
            		unProcessed = processTransaction(loanTransaction, currency, installments, amountToProcess);
            	
            	else if(loanTransaction.getLoan().getLoanProduct().isNewProductConfig())
            		unProcessed = processTransactionNewConfig(loanTransaction, currency, installments, amountToProcess);
            }
        }
        return unProcessed;
    }

    @Override
    public boolean isInterestFirstRepaymentScheduleTransactionProcessor() {
        return false;
    }

    @Override
    public void handleRefund(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {
        // TODO Auto-generated method stub
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();
        final Comparator<LoanRepaymentScheduleInstallment> byDate = new Comparator<LoanRepaymentScheduleInstallment>() {

            @Override
            public int compare(LoanRepaymentScheduleInstallment ord1, LoanRepaymentScheduleInstallment ord2) {
                return ord1.getDueDate().compareTo(ord2.getDueDate());
            }
        };
        Collections.sort(installments, Collections.reverseOrder(byDate));
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            Money outstanding = currentInstallment.getTotalOutstanding(currency);
            Money due = currentInstallment.getDue(currency);

            if (outstanding.isLessThan(due)) {
                transactionAmountUnprocessed = handleRefundTransactionPaymentOfInstallment(currentInstallment, loanTransaction,
                        transactionAmountUnprocessed, transactionMappings);

            }

            if (transactionAmountUnprocessed.isZero()) break;

        }

        final Set<LoanCharge> loanFees = extractFeeCharges(charges);
        final Set<LoanCharge> loanPenalties = extractPenaltyCharges(charges);
        Integer installmentNumber = null;

        final Money feeCharges = loanTransaction.getFeeChargesPortion(currency);
        if (feeCharges.isGreaterThanZero()) {
            undoChargesPaidAmountBy(loanTransaction, feeCharges, loanFees, installmentNumber);
        }

        final Money penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
        if (penaltyCharges.isGreaterThanZero()) {
            undoChargesPaidAmountBy(loanTransaction, penaltyCharges, loanPenalties, installmentNumber);
        }
        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
    }

    /**
     * Invoked when a there is a refund of an active loan or undo of an active
     * loan
     * 
     * Undoes principal, interest, fees and charges of this transaction based on
     * the repayment strategy
     * 
     * @param transactionMappings
     *            TODO
     * 
     */
    protected abstract Money handleRefundTransactionPaymentOfInstallment(final LoanRepaymentScheduleInstallment currentInstallment,
            final LoanTransaction loanTransaction, final Money transactionAmountUnprocessed,
            final List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    private void undoChargesPaidAmountBy(final LoanTransaction loanTransaction, final Money feeCharges, final Set<LoanCharge> charges,
            final Integer installmentNumber) {

        Money amountRemaining = feeCharges;
        while (amountRemaining.isGreaterThanZero()) {
            final LoanCharge paidCharge = findLatestPaidChargeFromUnOrderedSet(charges, feeCharges.getCurrency());

            if (paidCharge != null) {
                Money feeAmount = feeCharges.zero();

                final Money amountDeductedTowardsCharge = paidCharge.undoPaidOrPartiallyAmountBy(amountRemaining, installmentNumber,
                        feeAmount);
                if (amountDeductedTowardsCharge.isGreaterThanZero()) {

                    final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(loanTransaction, paidCharge, amountDeductedTowardsCharge
                            .getAmount().multiply(new BigDecimal(-1)), null);
                    loanTransaction.getLoanChargesPaid().add(loanChargePaidBy);

                    amountRemaining = amountRemaining.minus(amountDeductedTowardsCharge);
                }
            }
        }

    }

    private LoanCharge findLatestPaidChargeFromUnOrderedSet(final Set<LoanCharge> charges, MonetaryCurrency currency) {
        LoanCharge latestPaidCharge = null;
        LoanCharge installemntCharge = null;
        LoanInstallmentCharge chargePerInstallment = null;
        for (final LoanCharge loanCharge : charges) {
            boolean isPaidOrPartiallyPaid = loanCharge.isPaidOrPartiallyPaid(currency);
            if (isPaidOrPartiallyPaid && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee()) {
                    LoanInstallmentCharge paidLoanChargePerInstallment = loanCharge
                            .getLastPaidOrPartiallyPaidInstallmentLoanCharge(currency);
                    if (chargePerInstallment == null
                            || (paidLoanChargePerInstallment != null && chargePerInstallment.getRepaymentInstallment().getDueDate()
                                    .isBefore(paidLoanChargePerInstallment.getRepaymentInstallment().getDueDate()))) {
                        installemntCharge = loanCharge;
                        chargePerInstallment = paidLoanChargePerInstallment;
                    }
                } else if (latestPaidCharge == null || (loanCharge.isPaidOrPartiallyPaid(currency))
                        && loanCharge.getDueLocalDate().isAfter(latestPaidCharge.getDueLocalDate())) {
                    latestPaidCharge = loanCharge;
                }
            }
        }
        if (latestPaidCharge == null
                || (chargePerInstallment != null && latestPaidCharge.getDueLocalDate().isAfter(
                        chargePerInstallment.getRepaymentInstallment().getDueDate()))) {
            latestPaidCharge = installemntCharge;
        }

        return latestPaidCharge;
    }

    @Override
    public void processTransactionsFromDerivedFields(List<LoanTransaction> transactionsPostDisbursement, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {
        for (final LoanTransaction loanTransaction : transactionsPostDisbursement) {
            if (!loanTransaction.isAccrualTransaction()) {
                processTransactionFromDerivedFields(loanTransaction, currency, installments, charges);
            }
        }
    }

    private void processTransactionFromDerivedFields(final LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {
        Money principal = loanTransaction.getPrincipalPortion(currency);
        Money interest = loanTransaction.getInterestPortion(currency);
        if (loanTransaction.isInterestWaiver()) {
            interest = loanTransaction.getAmount(currency);
        }
        Money feeCharges = loanTransaction.getFeeChargesPortion(currency);
        Money penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        if (principal.isGreaterThanZero() || interest.isGreaterThanZero() || feeCharges.isGreaterThanZero()
                || penaltyCharges.isGreaterThanZero()) {
            for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
                if (currentInstallment.isNotFullyPaidOff()) {
                    if (penaltyCharges.isGreaterThanZero()) {
                        Money penaltyChargesPortion = Money.zero(currency);
                        if (loanTransaction.isWaiver()) {
                            penaltyChargesPortion = currentInstallment.waivePenaltyChargesComponent(transactionDate, penaltyCharges);
                        } else {
                            penaltyChargesPortion = currentInstallment.payPenaltyChargesComponent(transactionDate, penaltyCharges);
                        }
                        penaltyCharges = penaltyCharges.minus(penaltyChargesPortion);
                    }

                    if (feeCharges.isGreaterThanZero()) {
                        Money feeChargesPortion = Money.zero(currency);
                        if (loanTransaction.isWaiver()) {
                            feeChargesPortion = currentInstallment.waiveFeeChargesComponent(transactionDate, feeCharges);
                        } else {
                            feeChargesPortion = currentInstallment.payFeeChargesComponent(transactionDate, feeCharges);
                        }
                        feeCharges = feeCharges.minus(feeChargesPortion);
                    }

                    if (interest.isGreaterThanZero()) {
                        Money interestPortion = Money.zero(currency);
                        if (loanTransaction.isWaiver()) {
                            interestPortion = currentInstallment.waiveInterestComponent(transactionDate, interest);
                        } else {
                            interestPortion = currentInstallment.payInterestComponent(transactionDate, interest, loanTransaction.getLoan().getLoanProduct());
                        }
                        interest = interest.minus(interestPortion);
                    }

                    if (principal.isGreaterThanZero()) {
                        Money principalPortion = currentInstallment.payPrincipalComponent(transactionDate, principal);
                        principal = principal.minus(principalPortion);
                    }
                }
                if (!(principal.isGreaterThanZero() || interest.isGreaterThanZero() || feeCharges.isGreaterThanZero() || penaltyCharges
                        .isGreaterThanZero())) {
                    break;
                }
            }
        }

        final Set<LoanCharge> loanFees = extractFeeCharges(charges);
        final Set<LoanCharge> loanPenalties = extractPenaltyCharges(charges);
        Integer installmentNumber = null;
        if (loanTransaction.isChargePayment() && installments.size() == 1) {
            installmentNumber = installments.get(0).getInstallmentNumber();
        }

        if (loanTransaction.isNotWaiver()) {
            feeCharges = loanTransaction.getFeeChargesPortion(currency);
            penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
            if (feeCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, feeCharges, loanFees, installmentNumber);
            }

            if (penaltyCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, penaltyCharges, loanPenalties, installmentNumber);
            }
        }
    }

}