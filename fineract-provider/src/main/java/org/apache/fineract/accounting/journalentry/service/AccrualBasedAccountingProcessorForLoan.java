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
package org.apache.fineract.accounting.journalentry.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.fineract.accounting.closure.domain.GLClosure;
import org.apache.fineract.accounting.common.AccountingConstants.ACCRUAL_ACCOUNTS_FOR_LOAN;
import org.apache.fineract.accounting.common.AccountingConstants.CASH_ACCOUNTS_FOR_LOAN;
import org.apache.fineract.accounting.common.AccountingConstants.FINANCIAL_ACTIVITY;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO;
import org.apache.fineract.accounting.journalentry.data.LoanDTO;
import org.apache.fineract.accounting.journalentry.data.LoanTransactionDTO;
import org.apache.fineract.organisation.office.domain.Office;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccrualBasedAccountingProcessorForLoan implements AccountingProcessorForLoan {

    private final AccountingProcessorHelper helper;

    @Autowired
    public AccrualBasedAccountingProcessorForLoan(final AccountingProcessorHelper accountingProcessorHelper) {
        this.helper = accountingProcessorHelper;
    }

    @Override
    public void createJournalEntriesForLoan(final LoanDTO loanDTO, String note) {
        final GLClosure latestGLClosure = this.helper.getLatestClosureByBranch(loanDTO.getOfficeId());
        final Office office = this.helper.getOfficeById(loanDTO.getOfficeId());
        for (final LoanTransactionDTO loanTransactionDTO : loanDTO.getNewLoanTransactions()) {
            final Date transactionDate = loanTransactionDTO.getTransactionDate();
            this.helper.checkForBranchClosures(latestGLClosure, transactionDate);

            /** Handle Disbursements **/
            if (loanTransactionDTO.getTransactionType().isDisbursement()) {
                createJournalEntriesForDisbursements(loanDTO, loanTransactionDTO, office, note);
            }

            /*** Handle Accruals ***/
            if (loanTransactionDTO.getTransactionType().isAccrual()) {
                createJournalEntriesForAccruals(loanDTO, loanTransactionDTO, office, note);
            }

            /*** Habile Handle Income Poste ***/
            if (loanTransactionDTO.getTransactionType().isIncomePost()) {
                createJournalEntriesForIncomePost(loanDTO, loanTransactionDTO, office, note);
            }
            
            /***
             * Handle repayments, repayments at disbursement and reversal of
             * Repayments and Repayments at disbursement
             ***/
            else if (loanTransactionDTO.getTransactionType().isRepayment()
                    || loanTransactionDTO.getTransactionType().isRepaymentAtDisbursement()
                    || loanTransactionDTO.getTransactionType().isChargePayment()) {
                createJournalEntriesForRepaymentsAndWriteOffs(loanDTO, loanTransactionDTO, office, false, loanTransactionDTO
                        .getTransactionType().isRepaymentAtDisbursement(), note);
            }

            /** Logic for handling recovery payments **/
            else if (loanTransactionDTO.getTransactionType().isRecoveryRepayment()) {
                createJournalEntriesForRecoveryRepayments(loanDTO, loanTransactionDTO, office, note);
            }

            /** Logic for Refunds of Overpayments **/
            else if (loanTransactionDTO.getTransactionType().isRefund()) {
                createJournalEntriesForRefund(loanDTO, loanTransactionDTO, office, note);
            }

            /** Handle Write Offs, waivers and their reversals **/
            else if ((loanTransactionDTO.getTransactionType().isWriteOff() || loanTransactionDTO.getTransactionType().isWaiveInterest() || loanTransactionDTO
                    .getTransactionType().isWaiveCharges())) {
                createJournalEntriesForRepaymentsAndWriteOffs(loanDTO, loanTransactionDTO, office, true, false, note);
            } /** Handle Client Transfer Offs **/
            else if (loanTransactionDTO.getTransactionType().isInitiateTransfer()
                    || loanTransactionDTO.getTransactionType().isApproveTransfer()
                    || loanTransactionDTO.getTransactionType().isWithdrawTransfer()) {
                createJournalEntriesForTransfers(loanDTO, loanTransactionDTO, office, note);
            }
            /** Logic for Refunds of Active Loans **/
            else if (loanTransactionDTO.getTransactionType().isRefundForActiveLoans()) {
                createJournalEntriesForRefundForActiveLoan(loanDTO, loanTransactionDTO, office, note);
            }
        }
    }

    /**
     * Debit loan Portfolio and credit Fund source for Disbursement.
     * 
     * @param loanDTO
     * @param loanTransactionDTO
     * @param office
     */
    private void createJournalEntriesForDisbursements(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO,
            final Office office, String note) {

        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal disbursalAmount = loanTransactionDTO.getAmount();
        final boolean isReversed = loanTransactionDTO.isReversed();
        final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();
        String intDesc = "Disbursement ";
        if(note !=null) {
    	note = intDesc.concat(note);
        }
        
        // create journal entries for the disbursement (or disbursement
        // reversal)
        if(loanTransactionDTO.isLoanToLoanTransfer()){
        	/*if(!isReversed)
        		this.helper.checkClosingBalanceForCreditAmounts(FINANCIAL_ACTIVITY.ASSET_TRANSFER.getValue(), office.getId(), transactionDate, office.getHierarchy(), disbursalAmount, loanProductId, paymentTypeId);*/
            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), FINANCIAL_ACTIVITY.ASSET_TRANSFER.getValue(), loanProductId,
                    paymentTypeId, loanId, transactionId, transactionDate, disbursalAmount, isReversed, note);
        } else if (loanTransactionDTO.isAccountTransfer()) {
        	/*if(!isReversed)
        		this.helper.checkClosingBalanceForCreditAmounts(FINANCIAL_ACTIVITY.LIABILITY_TRANSFER.getValue(), office.getId(), transactionDate, office.getHierarchy(), disbursalAmount, loanProductId, paymentTypeId);*/
            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), FINANCIAL_ACTIVITY.LIABILITY_TRANSFER.getValue(), loanProductId,
                    paymentTypeId, loanId, transactionId, transactionDate, disbursalAmount, isReversed, note);
        } else {
        	/*if(!isReversed)
        		this.helper.checkClosingBalanceForCreditAmounts(ACCRUAL_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(), office.getId(), transactionDate, office.getHierarchy(), disbursalAmount, loanProductId, paymentTypeId);*/
            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(), loanProductId,
                    paymentTypeId, loanId, transactionId, transactionDate, disbursalAmount, isReversed, note);
        }

    }

    /**
     * 
     * Handles repayments using the following posting rules <br/>
     * <br/>
     * <br/>
     * 
     * <b>Principal Repayment</b>: Debits "Fund Source" and Credits
     * "Loan Portfolio"<br/>
     * 
     * <b>Interest Repayment</b>:Debits "Fund Source" and and Credits
     * "Receivable Interest" <br/>
     * 
     * <b>Fee Repayment</b>:Debits "Fund Source" (or "Interest on Loans" in case
     * of repayment at disbursement) and and Credits "Receivable Fees" <br/>
     * 
     * <b>Penalty Repayment</b>: Debits "Fund Source" and and Credits
     * "Receivable Penalties" <br/>
     * <br/>
     * Handles write offs using the following posting rules <br/>
     * <br/>
     * <b>Principal Write off</b>: Debits "Losses Written Off" and Credits
     * "Loan Portfolio"<br/>
     * 
     * <b>Interest Write off</b>:Debits "Losses Written off" and and Credits
     * "Receivable Interest" <br/>
     * 
     * <b>Fee Write off</b>:Debits "Losses Written off" and and Credits
     * "Receivable Fees" <br/>
     * 
     * <b>Penalty Write off</b>: Debits "Losses Written off" and and Credits
     * "Receivable Penalties" <br/>
     * <br/>
     * <br/>
     * In case the loan transaction has been reversed, all debits are turned
     * into credits and vice versa
     * 
     * @param loanTransactionDTO
     * @param loanDTO
     * @param office
     * 
     */
    private void createJournalEntriesForRepaymentsAndWriteOffs(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO,
            final Office office, final boolean writeOff, final boolean isIncomeFromFee, String note) {
        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal principalAmount = loanTransactionDTO.getPrincipal();
        final BigDecimal interestAmount = loanTransactionDTO.getInterest();
        final BigDecimal feesAmount = loanTransactionDTO.getFees();
        BigDecimal penaltiesAmount = loanTransactionDTO.getPenalties();
        final BigDecimal overPaymentAmount = loanTransactionDTO.getOverPayment();
        final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();
        final boolean isReversal = loanTransactionDTO.isReversed();

        BigDecimal totalDebitAmount = new BigDecimal(0);

        Map<GLAccount, BigDecimal> accountMap = new HashMap<>();
        
        if (loanTransactionDTO.getTransactionType().isRepaymentAtDisbursement()) {
			createJournalEntriesForRepayments(loanDTO, loanTransactionDTO, office,note);
		} else if (loanTransactionDTO.getTransactionType().isRepaymentAtInstallmentRescheduled()) {
			createJournalEntriesForRepayments(loanDTO, loanTransactionDTO, office,note);
		}

		else {

        // handle principal payment or writeOff (and reversals)
        if (principalAmount != null && !(principalAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(principalAmount);
            GLAccount account = this.helper.getLinkedGLAccountForLoanProduct(loanProductId,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), paymentTypeId);
            accountMap.put(account, principalAmount);
        }

        // handle interest payment of writeOff (and reversals)
        if (interestAmount != null && !(interestAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(interestAmount);
            GLAccount account = this.helper.getLinkedGLAccountForLoanProduct(loanProductId,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), paymentTypeId);
            if (accountMap.containsKey(account)) {
                BigDecimal amount = accountMap.get(account).add(interestAmount);
                accountMap.put(account, amount);
            } else {
                accountMap.put(account, interestAmount);
            }
        }

        // handle fees payment of writeOff (and reversals)
        if (feesAmount != null && !(feesAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(feesAmount);

            if (isIncomeFromFee) {
                GLAccount account = this.helper.getLinkedGLAccountForLoanProduct(loanProductId,
                        ACCRUAL_ACCOUNTS_FOR_LOAN.PROCESSING_FEE.getValue(), paymentTypeId);
                if (accountMap.containsKey(account)) {
                    BigDecimal amount = accountMap.get(account).add(feesAmount);
                    accountMap.put(account, amount);
                } else {
                    accountMap.put(account, feesAmount);
                }
            } else {
                GLAccount account = this.helper.getLinkedGLAccountForLoanProduct(loanProductId,
                        ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), paymentTypeId);
                if (accountMap.containsKey(account)) {
                    BigDecimal amount = accountMap.get(account).add(feesAmount);
                    accountMap.put(account, amount);
                } else {
                    accountMap.put(account, feesAmount);
                }
            }
        }

        // handle penalties payment of writeOff (and reversals)
        if (penaltiesAmount != null && !(penaltiesAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(penaltiesAmount);
            if (isIncomeFromFee) {
                GLAccount account = this.helper.getLinkedGLAccountForLoanProduct(loanProductId,
                        ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), paymentTypeId);
                if (accountMap.containsKey(account)) {
                    BigDecimal amount = accountMap.get(account).add(penaltiesAmount);
                    accountMap.put(account, amount);
                } else {
                    accountMap.put(account, penaltiesAmount);
                }
            } else {
                GLAccount account = this.helper.getLinkedGLAccountForLoanProduct(loanProductId,
                        ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), paymentTypeId);
                if (accountMap.containsKey(account)) {
                    BigDecimal amount = accountMap.get(account).add(penaltiesAmount);
                    accountMap.put(account, amount);
                } else {
                    accountMap.put(account, penaltiesAmount);
                }
            }
        }
            else {
				
				
				/**Habilen changes tax accounting for loan repayment*/
				Map<GLAccount, BigDecimal> taxAccounts = this.helper.calculatePenaltyTaxForAcccounting(loanId, loanTransactionDTO);
				BigDecimal penaltyTaxAmount = BigDecimal.ZERO;
				for (Entry<GLAccount, BigDecimal> entry : taxAccounts.entrySet()) {
					penaltyTaxAmount = penaltyTaxAmount.add(entry.getValue());
				}
				if(!taxAccounts.isEmpty()) {
				accountMap.putAll(taxAccounts);}
				/*penaltiesAmount = penaltiesAmount.subtract(penaltyTaxAmount);*/
				/**Habile changes end*/
        }

        if (overPaymentAmount != null && !(overPaymentAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(overPaymentAmount);
            GLAccount account = this.helper.getLinkedGLAccountForLoanProduct(loanProductId,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.OVERPAYMENT.getValue(), paymentTypeId);
            if (accountMap.containsKey(account)) {
                BigDecimal amount = accountMap.get(account).add(overPaymentAmount);
                accountMap.put(account, amount);
            } else {
                accountMap.put(account, overPaymentAmount);
            }
        }

        for (Entry<GLAccount, BigDecimal> entry : accountMap.entrySet()) {
            this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode, loanId, transactionId, transactionDate,
                    entry.getValue(), isReversal, entry.getKey(), note);
        }

        /**
         * Single DEBIT transaction for write-offs or Repayments (and their
         * reversals)
         ***/
        if (!(totalDebitAmount.compareTo(BigDecimal.ZERO) == 0)) {
            if (writeOff) {
                this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode,
                        ACCRUAL_ACCOUNTS_FOR_LOAN.LOSSES_WRITTEN_OFF.getValue(), loanProductId, paymentTypeId, loanId, transactionId,
                        transactionDate, totalDebitAmount, isReversal, note);
            } else {
                if(loanTransactionDTO.isLoanToLoanTransfer()){
                    this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode,
                            FINANCIAL_ACTIVITY.ASSET_TRANSFER.getValue(), loanProductId, paymentTypeId, loanId, transactionId,
                            transactionDate, totalDebitAmount, isReversal, note);
                } else if (loanTransactionDTO.isAccountTransfer()) {
                    this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode,
                            FINANCIAL_ACTIVITY.LIABILITY_TRANSFER.getValue(), loanProductId, paymentTypeId, loanId, transactionId,
                            transactionDate, totalDebitAmount, isReversal, note);
                } else {
                    this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode,
                            ACCRUAL_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(), loanProductId, paymentTypeId, loanId, transactionId,
                            transactionDate, totalDebitAmount, isReversal, note);
                }
            }
        }
		}
    }
     // HABILE CHANGES STARTS HERE

    	// Newly added method for repayment to reflect in the journal entry

    	private void createJournalEntriesForRepayments(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO,
    			final Office office,final String note) {
    		// loan properties
    		final Long loanProductId = loanDTO.getLoanProductId();
    		final Long loanId = loanDTO.getLoanId();
    		final String currencyCode = loanDTO.getCurrencyCode();

    		// transaction properties
    		final String transactionId = loanTransactionDTO.getTransactionId();
    		final Date transactionDate = loanTransactionDTO.getTransactionDate();
    		final BigDecimal principalAmount = loanTransactionDTO.getPrincipal();
    		final BigDecimal interestAmount = loanTransactionDTO.getInterest();
    		final BigDecimal feesAmount = loanTransactionDTO.getFees();
    		final BigDecimal penaltiesAmount = loanTransactionDTO.getPenalties();
    		final BigDecimal overPaymentAmount = loanTransactionDTO.getOverPayment();
    		final boolean isReversal = loanTransactionDTO.isReversed();
    		final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();

    		BigDecimal totalDebitAmount = new BigDecimal(0);

    		if (principalAmount != null && !(principalAmount.compareTo(BigDecimal.ZERO) == 0)) {
    			totalDebitAmount = totalDebitAmount.add(principalAmount);
    			this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode,
    					CASH_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO, loanProductId, paymentTypeId, loanId, transactionId,
    					transactionDate, principalAmount, isReversal,note);
    		}
    		//office, currencyCode, account, loanId, transactionId, transactionDate, amount

    		if (interestAmount != null && !(interestAmount.compareTo(BigDecimal.ZERO) == 0)) {
    			totalDebitAmount = totalDebitAmount.add(interestAmount);
    			this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode,
    					CASH_ACCOUNTS_FOR_LOAN.INTEREST_ON_LOANS, loanProductId, paymentTypeId, loanId, transactionId,
    					transactionDate, interestAmount, isReversal,note);
    		}

    		if (feesAmount != null && !(feesAmount.compareTo(BigDecimal.ZERO) == 0)) {
    			totalDebitAmount = totalDebitAmount.add(feesAmount);
    			this.helper.createCreditJournalEntryOrReversalForLoanCharges(office, currencyCode,
    					CASH_ACCOUNTS_FOR_LOAN.INCOME_FROM_FEES.getValue(), loanProductId, loanId, transactionId,
    					transactionDate, feesAmount, isReversal, loanTransactionDTO.getFeePayments(),note);
    		}

    		if (penaltiesAmount != null && !(penaltiesAmount.compareTo(BigDecimal.ZERO) == 0)) {
    			totalDebitAmount = totalDebitAmount.add(penaltiesAmount);
    			this.helper.createCreditJournalEntryOrReversalForLoanCharges(office, currencyCode,
    					CASH_ACCOUNTS_FOR_LOAN.INCOME_FROM_PENALTIES.getValue(), loanProductId, loanId, transactionId,
    					transactionDate, penaltiesAmount, isReversal, loanTransactionDTO.getPenaltyPayments(),note);
    		}

    		if (overPaymentAmount != null && !(overPaymentAmount.compareTo(BigDecimal.ZERO) == 0)) {
    			totalDebitAmount = totalDebitAmount.add(overPaymentAmount);
    			this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode,
    					CASH_ACCOUNTS_FOR_LOAN.OVERPAYMENT, loanProductId, paymentTypeId, loanId, transactionId,
    					transactionDate, overPaymentAmount, isReversal,note);
    		}

    		/*** create a single debit entry (or reversal) for the entire amount **/
    		if (loanTransactionDTO.isLoanToLoanTransfer()) {
    			this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode,
    					FINANCIAL_ACTIVITY.ASSET_TRANSFER.getValue(), loanProductId, paymentTypeId, loanId, transactionId,
    					transactionDate, totalDebitAmount, isReversal,note);
    		} else if (loanTransactionDTO.isAccountTransfer()) {
    			this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode,
    					FINANCIAL_ACTIVITY.LIABILITY_TRANSFER.getValue(), loanProductId, paymentTypeId, loanId,
    					transactionId, transactionDate, totalDebitAmount, isReversal, note);
    		} else {
    			this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode,
    					CASH_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(), loanProductId, paymentTypeId, loanId, transactionId,
    					transactionDate, totalDebitAmount, isReversal,note);
    		}
    	}
    	// HABILE CHANGES ENDS HERE
    /**
     * Create a single Debit to fund source and a single credit to
     * "Income from Recovery"
     * 
     * In case the loan transaction is a reversal, all debits are turned into
     * credits and vice versa
     */
    private void createJournalEntriesForRecoveryRepayments(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO,
            final Office office, String note) {
        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal amount = loanTransactionDTO.getAmount();
        final boolean isReversal = loanTransactionDTO.isReversed();
        final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();

        this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                ACCRUAL_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.INCOME_FROM_RECOVERY.getValue(), loanProductId,
                paymentTypeId, loanId, transactionId, transactionDate, amount, isReversal, note);

    }

    /**
     * Recognize the receivable interest <br/>
     * Debit "Interest Receivable" and Credit "Income from Interest"
     * 
     * <b>Fees:</b> Debit <i>Fees Receivable</i> and credit <i>Income from
     * Fees</i> <br/>
     * 
     * <b>Penalties:</b> Debit <i>Penalties Receivable</i> and credit <i>Income
     * from Penalties</i>
     * 
     * Also handles reversals for both fees and payment applications
     * 
     * @param loanDTO
     * @param loanTransactionDTO
     * @param office
     */
    private void createJournalEntriesForAccruals(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO, final Office office, String note) {

        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal interestAmount = loanTransactionDTO.getInterest();
        final BigDecimal feesAmount = loanTransactionDTO.getFees();
        final BigDecimal penaltiesAmount = loanTransactionDTO.getPenalties();
        final boolean isReversed = loanTransactionDTO.isReversed();
        final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();

        // create journal entries for recognizing interest (or reversal) 
        if (interestAmount != null && !(interestAmount.compareTo(BigDecimal.ZERO) == 0)) {
            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.INTEREST_RECEIVABLE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.ACCRUAL_AC.getValue(),
                    loanProductId, paymentTypeId, loanId, transactionId, transactionDate, interestAmount, isReversed, note);
        }
        /*// create journal entries for the fees application (or reversal)
        if (feesAmount != null && !(feesAmount.compareTo(BigDecimal.ZERO) == 0)) {
            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoanCharges(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.FEES_RECEIVABLE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.INCOME_FROM_FEES.getValue(),
                    loanProductId, loanId, transactionId, transactionDate, feesAmount, isReversed, loanTransactionDTO.getFeePayments(), note);
        }
        // create journal entries for the penalties application (or reversal)
        if (penaltiesAmount != null && !(penaltiesAmount.compareTo(BigDecimal.ZERO) == 0)) {

            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoanCharges(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.PENALTIES_RECEIVABLE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.INCOME_FROM_PENALTIES.getValue(),
                    loanProductId, loanId, transactionId, transactionDate, penaltiesAmount, isReversed,
                    loanTransactionDTO.getPenaltyPayments(), note);
        }*/
    }
    
    
    /** Habile Change For income Post **/
    private void createJournalEntriesForIncomePost(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO, final Office office, String note) {

        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal interestAmount = loanTransactionDTO.getInterest();
        final BigDecimal feesAmount = loanTransactionDTO.getFees();
        final BigDecimal penaltiesAmount = loanTransactionDTO.getPenalties();
        final Boolean isReversed = loanTransactionDTO.isReversed();
        final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();
        BigDecimal totalDebitAmount = new BigDecimal(0);
        
        if(note ==null) {
        	note = " for the loan " + loanId +" of the client ";
        }

        String debitNote="";
        // create journal entries for recognizing interest (or reversal)
        if (interestAmount != null && !(interestAmount.compareTo(BigDecimal.ZERO) == 0)) {
        	String intDesc = "Interest collected ";
        	intDesc = intDesc.concat(note);
        	debitNote ="Interest ";
        	debitNote = debitNote.concat(note);
        	 totalDebitAmount = totalDebitAmount.add(interestAmount);
             this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode, CASH_ACCOUNTS_FOR_LOAN.INTEREST_ON_LOANS,
                     loanProductId, paymentTypeId, loanId, transactionId, transactionDate, interestAmount, isReversed, intDesc);
           /* this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.INTEREST_ON_LOANS.getValue(),
                    loanProductId, paymentTypeId, loanId, transactionId, transactionDate, interestAmount, isReversed, intDesc);*/
            
         }
        // create journal entries for the fees application (or reversal)
        if (feesAmount != null && !(feesAmount.compareTo(BigDecimal.ZERO) == 0)) {
        	String feeDesc = "Fees collected ";
        	feeDesc = feeDesc.concat(note);
        	debitNote ="Fees ";
        	debitNote = debitNote.concat(note);
        	totalDebitAmount = totalDebitAmount.add(feesAmount);
        	 this.helper.createCreditJournalEntryOrReversalForLoanCharges(office, currencyCode,
                     CASH_ACCOUNTS_FOR_LOAN.INCOME_FROM_FEES.getValue(), loanProductId, loanId, transactionId, transactionDate, feesAmount,
                     isReversed, loanTransactionDTO.getFeePayments(), feeDesc);
        	/*this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    ACCRUAL_debitNoteACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.FEES_RECEIVED.getValue(),
                    loanProductId, paymentTypeId,loanId,transactionId, transactionDate, feesAmount, isReversed, feeDesc);*/
        }
        // create journal entries for the penalties application (or reversal)
        if (penaltiesAmount != null && !(penaltiesAmount.compareTo(BigDecimal.ZERO) == 0)) {
        	String penaltyDesc = "Penalties collected";
        	debitNote ="Penalty ";
        	debitNote = debitNote.concat(note);
        	penaltyDesc = penaltyDesc.concat(note);
        	totalDebitAmount = totalDebitAmount.add(penaltiesAmount);
        	this.helper.createCreditJournalEntryOrReversalForLoanCharges(office, currencyCode,
                     CASH_ACCOUNTS_FOR_LOAN.INCOME_FROM_PENALTIES.getValue(), loanProductId, loanId, transactionId, transactionDate,
                     penaltiesAmount, isReversed, loanTransactionDTO.getPenaltyPayments(), penaltyDesc);
           /* this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.PENALITY_RECEIVED.getValue(),
                    loanProductId, paymentTypeId, loanId, transactionId, transactionDate, penaltiesAmount, isReversed, penaltyDesc);*/
        }
        
        this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode, ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(),
                loanProductId, paymentTypeId, loanId, transactionId, transactionDate, totalDebitAmount, isReversed, debitNote);
    }

    private void createJournalEntriesForRefund(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO, final Office office, String note) {
        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal refundAmount = loanTransactionDTO.getAmount();
        final boolean isReversal = loanTransactionDTO.isReversed();
        final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();

        if (loanTransactionDTO.isAccountTransfer()) {
            this.helper.createCashBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    CASH_ACCOUNTS_FOR_LOAN.OVERPAYMENT.getValue(), FINANCIAL_ACTIVITY.LIABILITY_TRANSFER.getValue(), loanProductId,
                    paymentTypeId, loanId, transactionId, transactionDate, refundAmount, isReversal, note);
        } else {
            this.helper.createCashBasedJournalEntriesAndReversalsForLoan(office, currencyCode,
                    CASH_ACCOUNTS_FOR_LOAN.OVERPAYMENT.getValue(), CASH_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(), loanProductId,
                    paymentTypeId, loanId, transactionId, transactionDate, refundAmount, isReversal, note);
        }
    }

    private void createJournalEntriesForRefundForActiveLoan(LoanDTO loanDTO, LoanTransactionDTO loanTransactionDTO, Office office, String note) {
        // TODO Auto-generated method stub
        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal principalAmount = loanTransactionDTO.getPrincipal();
        final BigDecimal interestAmount = loanTransactionDTO.getInterest();
        final BigDecimal feesAmount = loanTransactionDTO.getFees();
        final BigDecimal penaltiesAmount = loanTransactionDTO.getPenalties();
        final BigDecimal overPaymentAmount = loanTransactionDTO.getOverPayment();
        final boolean isReversal = loanTransactionDTO.isReversed();
        final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();

        BigDecimal totalDebitAmount = new BigDecimal(0);

        if (principalAmount != null && !(principalAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(principalAmount);
            this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode, CASH_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO,
                    loanProductId, paymentTypeId, loanId, transactionId, transactionDate, principalAmount, !isReversal, note);
        }

        if (interestAmount != null && !(interestAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(interestAmount);
            this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode, CASH_ACCOUNTS_FOR_LOAN.INTEREST_ON_LOANS,
                    loanProductId, paymentTypeId, loanId, transactionId, transactionDate, interestAmount, !isReversal, note);
        }

        if (feesAmount != null && !(feesAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(feesAmount);

            List<ChargePaymentDTO> chargePaymentDTOs = new ArrayList<>();

            for (ChargePaymentDTO chargePaymentDTO : loanTransactionDTO.getFeePayments()) {
                chargePaymentDTOs.add(new ChargePaymentDTO(chargePaymentDTO.getChargeId(), chargePaymentDTO.getLoanChargeId(),
                        chargePaymentDTO.getAmount().floatValue() < 0 ? chargePaymentDTO.getAmount().multiply(new BigDecimal(-1))
                                : chargePaymentDTO.getAmount()));
            }
            this.helper.createCreditJournalEntryOrReversalForLoanCharges(office, currencyCode,
                    CASH_ACCOUNTS_FOR_LOAN.INCOME_FROM_FEES.getValue(), loanProductId, loanId, transactionId, transactionDate, feesAmount,
                    !isReversal, chargePaymentDTOs, note);
        }

        if (penaltiesAmount != null && !(penaltiesAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(penaltiesAmount);
            List<ChargePaymentDTO> chargePaymentDTOs = new ArrayList<>();

            for (ChargePaymentDTO chargePaymentDTO : loanTransactionDTO.getPenaltyPayments()) {
                chargePaymentDTOs.add(new ChargePaymentDTO(chargePaymentDTO.getChargeId(), chargePaymentDTO.getLoanChargeId(),
                        chargePaymentDTO.getAmount().floatValue() < 0 ? chargePaymentDTO.getAmount().multiply(new BigDecimal(-1))
                                : chargePaymentDTO.getAmount()));
            }

            this.helper.createCreditJournalEntryOrReversalForLoanCharges(office, currencyCode,
                    CASH_ACCOUNTS_FOR_LOAN.INCOME_FROM_PENALTIES.getValue(), loanProductId, loanId, transactionId, transactionDate,
                    penaltiesAmount, !isReversal, chargePaymentDTOs, note);
        }

        if (overPaymentAmount != null && !(overPaymentAmount.compareTo(BigDecimal.ZERO) == 0)) {
            totalDebitAmount = totalDebitAmount.add(overPaymentAmount);
            this.helper.createCreditJournalEntryOrReversalForLoan(office, currencyCode, CASH_ACCOUNTS_FOR_LOAN.OVERPAYMENT, loanProductId,
                    paymentTypeId, loanId, transactionId, transactionDate, overPaymentAmount, !isReversal, note);
        }

        /*** create a single debit entry (or reversal) for the entire amount **/
        this.helper.createDebitJournalEntryOrReversalForLoan(office, currencyCode, CASH_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(),
                loanProductId, paymentTypeId, loanId, transactionId, transactionDate, totalDebitAmount, !isReversal, note);

    }
    
    
    /**
     * Credit loan Portfolio and Debit Suspense Account for a Transfer
     * Initiation. A Transfer acceptance would be treated the opposite i.e Debit
     * Loan Portfolio and Credit Suspense Account <br/>
     * 
     * All debits are turned into credits and vice versa in case of Transfer
     * Initiation disbursals
     * 
     * 
     * @param loanDTO
     * @param loanTransactionDTO
     * @param office
     */
    private void createJournalEntriesForTransfers(final LoanDTO loanDTO, final LoanTransactionDTO loanTransactionDTO, final Office office, String note) {
        // loan properties
        final Long loanProductId = loanDTO.getLoanProductId();
        final Long loanId = loanDTO.getLoanId();
        final String currencyCode = loanDTO.getCurrencyCode();

        // transaction properties
        final String transactionId = loanTransactionDTO.getTransactionId();
        final Date transactionDate = loanTransactionDTO.getTransactionDate();
        final BigDecimal principalAmount = loanTransactionDTO.getPrincipal();
        final BigDecimal interestAmount = loanTransactionDTO.getInterest();
        final BigDecimal feeAmount = loanTransactionDTO.getFees();
        final BigDecimal penaltyAmount = loanTransactionDTO.getPenalties();
        final boolean isReversal = loanTransactionDTO.isReversed();
        final Office destinationOffice =  this.helper.getOfficeById(loanTransactionDTO.getOfficeId());
        // final Long paymentTypeId = loanTransactionDTO.getPaymentTypeId();

        if (loanTransactionDTO.getTransactionType().isInitiateTransfer()) {
        	  if (principalAmount != null && !(principalAmount.compareTo(BigDecimal.ZERO) == 0)) {
		            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
		            		ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), loanProductId,
		                    null, loanId, transactionId, transactionDate, principalAmount, isReversal, note);
        	  }
        	  if (interestAmount != null && !(interestAmount.compareTo(BigDecimal.ZERO) == 0)) {
		            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
		            		ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.INTEREST_RECEIVABLE.getValue(), loanProductId,
		                    null, loanId, transactionId, transactionDate, interestAmount, isReversal, note);
        	  }
        	  if (feeAmount != null && !(feeAmount.compareTo(BigDecimal.ZERO) == 0)) {
		            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
		            		ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.FEES_RECEIVABLE.getValue(), loanProductId,
		                    null, loanId, transactionId, transactionDate, feeAmount, isReversal, note);
        	  }
        	  if (penaltyAmount != null && !(penaltyAmount.compareTo(BigDecimal.ZERO) == 0)) {
		            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
		            		ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.PENALTIES_RECEIVABLE.getValue(), loanProductId,
		                    null, loanId, transactionId, transactionDate, penaltyAmount, isReversal, note);
        	  }
        } else if (loanTransactionDTO.getTransactionType().isApproveTransfer()
                || loanTransactionDTO.getTransactionType().isWithdrawTransfer()) {
        	if (principalAmount != null && !(principalAmount.compareTo(BigDecimal.ZERO) == 0)) {
        	this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
            		ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), loanProductId,
                    null, loanId, transactionId, transactionDate, principalAmount, isReversal, note);
        	}
        	if (interestAmount != null && !(interestAmount.compareTo(BigDecimal.ZERO) == 0)) {
	            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
	            		ACCRUAL_ACCOUNTS_FOR_LOAN.INTEREST_RECEIVABLE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), loanProductId,
	                    null, loanId, transactionId, transactionDate, interestAmount, isReversal, note);
        	}
    	  if (feeAmount != null && !(feeAmount.compareTo(BigDecimal.ZERO) == 0)) {
	            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
	            		ACCRUAL_ACCOUNTS_FOR_LOAN.FEES_RECEIVABLE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), loanProductId,
	                    null, loanId, transactionId, transactionDate, feeAmount, isReversal, note);
    	  }
    	  if (penaltyAmount != null && !(penaltyAmount.compareTo(BigDecimal.ZERO) == 0)) {
	            this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(destinationOffice, currencyCode,
	            		ACCRUAL_ACCOUNTS_FOR_LOAN.PENALTIES_RECEIVABLE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.TRANSFERS_SUSPENSE.getValue(), loanProductId,
	                    null, loanId, transactionId, transactionDate, penaltyAmount, isReversal, note);
    	  }
        }
    }
}
