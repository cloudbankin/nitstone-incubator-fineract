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
package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

import java.math.MathContext;
import java.util.Set;

import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.loanaccount.data.HolidayDetailDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleDTO;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.joda.time.LocalDate;

public interface LoanScheduleGenerator {

    /*LoanScheduleModel generate(MathContext mc, LoanApplicationTerms loanApplicationTerms, Set<LoanCharge> loanCharges,
            final HolidayDetailDTO holidayDetailDTO);*/

    LoanScheduleDTO rescheduleNextInstallments(MathContext mc, LoanApplicationTerms loanApplicationTerms, Loan loan,
            final HolidayDetailDTO holidayDetailDTO, LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor,
            LocalDate rescheduleFrom);

    LoanRepaymentScheduleInstallment calculatePrepaymentAmount(MonetaryCurrency currency, LocalDate onDate,
            LoanApplicationTerms loanApplicationTerms, MathContext mc, Loan loan, HolidayDetailDTO holidayDetailDTO,
            LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor);
    
    // Habile Arivu new config
    LoanScheduleModel generate(MathContext mc, LoanApplicationTerms loanApplicationTerms, Set<LoanCharge> loanCharges,
            final HolidayDetailDTO holidayDetailDTO,final String istrue, final LoanProduct loanProduct);

}