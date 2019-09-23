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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.util.Collection;

import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.joda.time.LocalDate;

/**
 * Immutable data object for loan charge data.
 */
public class LoanManualChargeData {

    private final Long id;
   
    private final Long loanId;

    private final String accountName;

    private final BigDecimal amount;

    private final BigDecimal amountPaid;
   
    private final boolean paid;

    private final String description;
    
    private final LocalDate chargeDate;

    public LoanManualChargeData(final Long id, final Long loanId, final String accountName, final BigDecimal amount, 
    	   final BigDecimal amountPaid, final boolean paid, final String description, final LocalDate chargeDate) {
    
    	this.id =id;
    	this.loanId = loanId;
    	this.accountName = accountName;
    	this.amount = amount;
    	this.amountPaid = amountPaid;
    	this.paid =paid;
    	this.description = description;
    	this.chargeDate = chargeDate;
    	
    }

   
}